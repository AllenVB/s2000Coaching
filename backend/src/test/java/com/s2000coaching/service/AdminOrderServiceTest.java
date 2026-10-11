package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	private AdminOrderService adminOrderService;
	private Member member;
	private CoachingPackage coachingPackage;

	@BeforeEach
	void setUp() {
		adminOrderService = new AdminOrderService(orderRepository);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
		coachingPackage = new CoachingPackage(UUID.randomUUID(), "kocluk-1-ay", PackageCategory.KOCLUK,
				"1 Ay Koçluk", null, 1, 129000L, 149000L, null, false, 1, true);
	}

	private Order order(String email) {
		return new Order(UUID.randomUUID(), "S2K-TEST0001", coachingPackage, "Ahmet Yılmaz", email,
				"+905551112233", "30, İzmir", 129000L, OrderStatus.MOCK_PAID, true, Instant.now());
	}

	@Test
	void shouldAttachUnlinkedOrdersPlacedWithTheMembersAddress() {
		Order unlinked = order("sporcu@example.com");
		when(orderRepository.findAllByEmailIgnoreCaseAndMemberIsNull("sporcu@example.com"))
				.thenReturn(List.of(unlinked));

		assertThat(adminOrderService.linkOrdersTo(member)).isEqualTo(1);
		assertThat(unlinked.getMember()).isSameAs(member);
	}

	@Test
	void shouldReportZeroWhenTheBuyerHasNoOrders() {
		when(orderRepository.findAllByEmailIgnoreCaseAndMemberIsNull("sporcu@example.com"))
				.thenReturn(List.of());

		assertThat(adminOrderService.linkOrdersTo(member)).isZero();
	}

	@Test
	void shouldDetachOrdersWithoutDeletingThem() {
		Order linked = order("sporcu@example.com");
		linked.setMember(member);
		when(orderRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId()))
				.thenReturn(List.of(linked));

		adminOrderService.unlinkOrdersOf(member.getId());

		assertThat(linked.getMember()).isNull();
		// The row itself is untouched — an order is a financial record.
		assertThat(linked.getOrderNumber()).isEqualTo("S2K-TEST0001");
	}

	@Test
	void shouldExposeWhetherAnOrderBelongsToAMember() {
		Order linked = order("sporcu@example.com");
		linked.setMember(member);
		when(orderRepository.findByOrderNumber("S2K-TEST0001")).thenReturn(Optional.of(linked));

		assertThat(adminOrderService.getByOrderNumber("S2K-TEST0001").memberId()).isEqualTo(member.getId());
	}

	@Test
	void shouldFailForAnUnknownOrder() {
		when(orderRepository.findByOrderNumber("S2K-YOK")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adminOrderService.getByOrderNumber("S2K-YOK"))
				.isInstanceOf(NotFoundException.class);
	}
}
