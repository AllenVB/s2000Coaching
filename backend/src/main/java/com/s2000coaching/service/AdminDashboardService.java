package com.s2000coaching.service;

import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.web.dto.AdminDashboardResponse;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.OrderSummaryResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Feeds the management panel: counts, who is waiting on confirmation, what just sold. */
@Service
public class AdminDashboardService {

	/** A member who has not checked in for this long is the one the coach should chase. */
	static final int STALE_AFTER_DAYS = 10;

	private final MemberRepository memberRepository;
	private final OrderRepository orderRepository;
	private final MeasurementRepository measurementRepository;

	public AdminDashboardService(MemberRepository memberRepository, OrderRepository orderRepository,
			MeasurementRepository measurementRepository) {
		this.memberRepository = memberRepository;
		this.orderRepository = orderRepository;
		this.measurementRepository = measurementRepository;
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

		var staleIds = measurementRepository.findActiveMemberIdsWithoutMeasurementSince(
				LocalDate.now().minusDays(STALE_AFTER_DAYS));
		var stale = memberRepository.findAllById(staleIds).stream()
				.map(MemberResponse::from)
				.toList();

		return new AdminDashboardResponse(
				memberRepository.count(),
				memberRepository.countByStatus(MemberStatus.ACTIVE),
				STALE_AFTER_DAYS,
				pending,
				stale,
				recentOrders);
	}
}
