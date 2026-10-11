package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private MeasurementRepository measurementRepository;

	@Mock
	private MessageService messageService;

	private AdminDashboardService adminDashboardService;

	@BeforeEach
	void setUp() {
		adminDashboardService = new AdminDashboardService(memberRepository, orderRepository,
				measurementRepository, messageService);
	}

	@Test
	void shouldReportCountsAndWhoIsWaitingOnConfirmation() {
		Member pending = new Member(UUID.randomUUID(), "bekleyen@example.com", "Bekleyen Üye", "+905551112233",
				180, new BigDecimal("80.00"), "Fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		when(memberRepository.count()).thenReturn(5L);
		when(memberRepository.countByStatus(MemberStatus.ACTIVE)).thenReturn(4L);
		when(memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.PENDING_VERIFICATION))
				.thenReturn(List.of(pending));
		when(orderRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());
		when(measurementRepository.findActiveMemberIdsWithoutMeasurementSince(any())).thenReturn(List.of());
		when(memberRepository.findAllById(List.of())).thenReturn(List.of());
		when(messageService.memberIdsAwaitingReply()).thenReturn(List.of());

		var dashboard = adminDashboardService.load();

		assertThat(dashboard.totalMembers()).isEqualTo(5);
		assertThat(dashboard.activeMembers()).isEqualTo(4);
		assertThat(dashboard.pendingMembers()).singleElement()
				.satisfies(member -> assertThat(member.email()).isEqualTo("bekleyen@example.com"));
		assertThat(dashboard.recentOrders()).isEmpty();
		assertThat(dashboard.staleMembers()).isEmpty();
		assertThat(dashboard.awaitingReply()).isEmpty();
		assertThat(dashboard.staleAfterDays()).isPositive();
	}
}
