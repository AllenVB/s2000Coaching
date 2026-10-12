package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.Order;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.web.dto.OrderSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coach-side view of orders, and the link between an order and the member it belongs to.
 *
 * <p>Checkout collects a name, e-mail and phone but creates no member, so the same person
 * used to be typed in twice. Orders are now attached to a member by e-mail as soon as that
 * member exists.
 */
@Service
public class AdminOrderService {

	private final OrderRepository orderRepository;

	public AdminOrderService(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Transactional(readOnly = true)
	public List<OrderSummaryResponse> listAll() {
		return orderRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(OrderSummaryResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<OrderSummaryResponse> listForMember(UUID memberId) {
		return orderRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.map(OrderSummaryResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public OrderSummaryResponse getByOrderNumber(String orderNumber) {
		return OrderSummaryResponse.from(orderRepository.findByOrderNumber(orderNumber)
				.orElseThrow(() -> new NotFoundException("Sipariş bulunamadı: " + orderNumber)));
	}

	/**
	 * Attaches every unlinked order placed with the member's address, so a buyer's order
	 * history is there from the moment they become a member.
	 *
	 * @return how many orders were attached
	 */
	@Transactional
	public int linkOrdersTo(Member member) {
		List<Order> orders = orderRepository.findAllByEmailIgnoreCaseAndMemberIsNull(member.getEmail());
		orders.forEach(order -> order.setMember(member));
		return orders.size();
	}

	/**
	 * Detaches a member's orders instead of deleting them: an order is a financial record and
	 * outlives the membership.
	 */
	@Transactional
	public void unlinkOrdersOf(UUID memberId) {
		orderRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
				.forEach(order -> order.setMember(null));
	}
}
