package com.s2000coaching.service;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.payment.PaymentGateway;
import com.s2000coaching.payment.PaymentResult;
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
	private final PaymentGateway paymentGateway;

	public OrderService(OrderRepository orderRepository, PackageService packageService,
			PaymentGateway paymentGateway) {
		this.orderRepository = orderRepository;
		this.packageService = packageService;
		this.paymentGateway = paymentGateway;
	}

	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
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

		PaymentResult result = paymentGateway.charge(order);
		order.setStatus(result.success() ? OrderStatus.MOCK_PAID : OrderStatus.FAILED);

		Order saved = orderRepository.save(order);
		return OrderResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public OrderResponse getByOrderNumber(String orderNumber) {
		Order order = orderRepository.findByOrderNumber(orderNumber)
				.orElseThrow(() -> new NotFoundException("Sipariş bulunamadı: " + orderNumber));
		return OrderResponse.from(order);
	}

	private String generateOrderNumber() {
		StringBuilder sb = new StringBuilder("S2K-");
		for (int i = 0; i < 8; i++) {
			sb.append(ORDER_NUMBER_ALPHABET.charAt(RANDOM.nextInt(ORDER_NUMBER_ALPHABET.length())));
		}
		return sb.toString();
	}
}
