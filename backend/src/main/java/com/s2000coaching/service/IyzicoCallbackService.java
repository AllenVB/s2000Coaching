package com.s2000coaching.service;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.payment.IyzicoClient;
import com.s2000coaching.payment.PaymentFailedException;
import com.s2000coaching.repository.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What happens when iyzico sends the buyer's browser back. The callback carries only a token,
 * and a token is not proof of payment: this asks iyzico, server to server, what became of it,
 * and only then settles the order.
 *
 * <p>An order is marked paid only if every check passes: the provider reports success with an
 * authentic response, the payment was opened for <em>this</em> order's number, and the amount
 * charged equals the order's price. Anything else leaves the order unpaid and, if the answer was
 * a refusal, marks it failed.
 */
@Service
public class IyzicoCallbackService {

	private static final Logger log = LoggerFactory.getLogger(IyzicoCallbackService.class);

	public enum Outcome {
		PAID,
		FAILED,
		UNKNOWN
	}

	public record Result(Outcome outcome, String orderNumber) {
	}

	private final OrderRepository orderRepository;
	private final IyzicoClient client;
	private final MembershipService membershipService;
	private final Clock clock;

	public IyzicoCallbackService(OrderRepository orderRepository, IyzicoClient client,
			MembershipService membershipService, Clock clock) {
		this.orderRepository = orderRepository;
		this.client = client;
		this.membershipService = membershipService;
		this.clock = clock;
	}

	@Transactional
	public Result complete(String token) {
		if (token == null || token.isBlank()) {
			return new Result(Outcome.UNKNOWN, null);
		}
		Optional<Order> found = orderRepository.findByPaymentReference(token);
		if (found.isEmpty()) {
			return new Result(Outcome.UNKNOWN, null);
		}
		Order order = found.get();

		// The browser can come back twice (refresh, back button); the second visit changes nothing.
		if (order.getStatus() == OrderStatus.PAID) {
			return new Result(Outcome.PAID, order.getOrderNumber());
		}
		if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
			return new Result(order.getStatus() == OrderStatus.FAILED ? Outcome.FAILED : Outcome.UNKNOWN,
					order.getOrderNumber());
		}

		IyzicoClient.Result answer;
		try {
			answer = client.retrieve(token);
		}
		catch (PaymentFailedException e) {
			// Could not ask: leave the order waiting rather than guess either way.
			log.error("iyzico sonucu alınamadı: sipariş {}", order.getOrderNumber(), e);
			return new Result(Outcome.UNKNOWN, order.getOrderNumber());
		}

		boolean sameOrder = order.getOrderNumber().equals(answer.basketId());
		boolean sameAmount = answer.paidPrice() != null
				&& answer.paidPrice().movePointRight(2).compareTo(java.math.BigDecimal.valueOf(order.getPriceMinor())) == 0;

		if (answer.success() && sameOrder && sameAmount) {
			order.markPaid(Instant.now(clock));
			membershipService.grantFor(order);
			return new Result(Outcome.PAID, order.getOrderNumber());
		}
		if (answer.success()) {
			// Paid, but not what we expected: do not call it paid, and make the coach look.
			log.error("iyzico ödemesi sipariş {} ile uyuşmuyor (basketId={}, tutar={}); sipariş ödenmedi sayılmadı.",
					order.getOrderNumber(), answer.basketId(), answer.paidPrice());
			return new Result(Outcome.UNKNOWN, order.getOrderNumber());
		}
		order.setStatus(OrderStatus.FAILED);
		return new Result(Outcome.FAILED, order.getOrderNumber());
	}
}
