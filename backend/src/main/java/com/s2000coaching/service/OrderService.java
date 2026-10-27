package com.s2000coaching.service;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PaymentMethod;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.payment.BankDetails;
import com.s2000coaching.payment.PaymentContext;
import com.s2000coaching.payment.PaymentGateway;
import com.s2000coaching.payment.PaymentInitiation;
import com.s2000coaching.payment.PaymentMethods;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.web.dto.CreateOrderRequest;
import com.s2000coaching.web.dto.OrderResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final String ORDER_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final OrderRepository orderRepository;
	private final PackageService packageService;
	private final PaymentMethods paymentMethods;

	public OrderService(OrderRepository orderRepository, PackageService packageService,
			PaymentMethods paymentMethods) {
		this.orderRepository = orderRepository;
		this.packageService = packageService;
		this.paymentMethods = paymentMethods;
	}

	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request, String clientIp) {
		CoachingPackage coachingPackage = packageService.getActiveOrThrow(request.packageId());

		Order order = new Order(
				UUID.randomUUID(),
				generateOrderNumber(),
				coachingPackage,
				request.customerName(),
				request.email(),
				request.phone(),
				request.cityAge(),
				coachingPackage.getPriceMinor(),
				OrderStatus.PENDING,
				request.kvkkConsent(),
				Instant.now());

		// Resolved before anything is saved: an unavailable method must not leave an order behind.
		PaymentGateway gateway = paymentMethods.resolve(request.paymentMethod());
		PaymentInitiation initiation = gateway.initiate(order, new PaymentContext(clientIp));
		order.setPaymentMethod(gateway.method());
		order.setStatus(initiation.status());
		order.setPaymentReference(initiation.reference());

		Order saved = orderRepository.save(order);
		return respond(saved, initiation.redirectUrl());
	}

	@Transactional(readOnly = true)
	public OrderResponse getByOrderNumber(String orderNumber) {
		Order order = orderRepository.findByOrderNumber(orderNumber)
				.orElseThrow(() -> new NotFoundException("Sipariş bulunamadı: " + orderNumber));
		return respond(order, null);
	}

	private OrderResponse respond(Order order, String redirectUrl) {
		BankDetails bank = order.getPaymentMethod() == PaymentMethod.IBAN
				? paymentMethods.bankDetails(order.getOrderNumber())
				: null;
		return OrderResponse.from(order, redirectUrl, bank);
	}

	private String generateOrderNumber() {
		StringBuilder sb = new StringBuilder("S2K-");
		for (int i = 0; i < 8; i++) {
			sb.append(ORDER_NUMBER_ALPHABET.charAt(RANDOM.nextInt(ORDER_NUMBER_ALPHABET.length())));
		}
		return sb.toString();
	}
}
