package com.s2000coaching.service;

import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.web.dto.AdminDashboardResponse;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.OrderSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Feeds the management panel: counts, who is waiting on confirmation, what just sold. */
@Service
public class AdminDashboardService {

	private final MemberRepository memberRepository;
	private final OrderRepository orderRepository;

	public AdminDashboardService(MemberRepository memberRepository, OrderRepository orderRepository) {
		this.memberRepository = memberRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional(readOnly = true)
	public AdminDashboardResponse load() {
		var pending = memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.PENDING_VERIFICATION)
				.stream()
				.map(MemberResponse::from)
				.toList();
		var recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc().stream()
				.map(OrderSummaryResponse::from)
				.toList();

		return new AdminDashboardResponse(
				memberRepository.count(),
				memberRepository.countByStatus(MemberStatus.ACTIVE),
				pending,
				recentOrders);
	}
}
