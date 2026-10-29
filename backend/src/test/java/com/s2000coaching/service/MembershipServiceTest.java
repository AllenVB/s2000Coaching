package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.Membership;
import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MembershipRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	@Mock
	private MembershipRepository membershipRepository;

	@Mock
	private MemberRepository memberRepository;

	private MembershipService service;
	private Member member;
	private Order order;

	@BeforeEach
	void setUp() {
		service = new MembershipService(membershipRepository, memberRepository,
				Clock.fixed(Instant.parse("2026-10-10T09:00:00Z"), ZoneOffset.UTC));
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
		var pack = new CoachingPackage(UUID.randomUUID(), "kocluk-3-ay", PackageCategory.KOCLUK, "3 Aylık Koçluk",
				null, 3, 400000L, 450000L, null, false, 1, true);
		order = new Order(UUID.randomUUID(), "S2K-TEST0001", pack, "Ahmet", "a@example.com", "+905551112233", null,
				400000L, OrderStatus.PAID, true, Instant.now());
		order.setMember(member);
	}

	private Membership saved() {
		ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
		verify(membershipRepository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void shouldStartTodayForAFirstMembershipAndRunForThePackagesMonths() {
		when(membershipRepository.findAllByMemberIdOrderByEndsOnDesc(member.getId())).thenReturn(List.of());
		when(membershipRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		service.grantFor(order);

		assertThat(saved().getStartsOn()).isEqualTo(TODAY);
		assertThat(saved().getEndsOn()).isEqualTo(TODAY.plusMonths(3));
		assertThat(saved().getLabel()).isEqualTo("3 Aylık Koçluk");
	}

	/** Renewing before the end must not throw away the days already paid for. */
	@Test
	void shouldStartWhenThePreviousPeriodEndsIfItIsStillRunning() {
		var running = new Membership(UUID.randomUUID(), member, null, "x", TODAY.minusMonths(1),
				TODAY.plusDays(20), Instant.now());
		when(membershipRepository.findAllByMemberIdOrderByEndsOnDesc(member.getId())).thenReturn(List.of(running));
		when(membershipRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		service.grantFor(order);

		assertThat(saved().getStartsOn()).isEqualTo(TODAY.plusDays(20));
		assertThat(saved().getEndsOn()).isEqualTo(TODAY.plusDays(20).plusMonths(3));
	}

	@Test
	void shouldStartTodayWhenThePreviousPeriodAlreadyEnded() {
		var ended = new Membership(UUID.randomUUID(), member, null, "x", TODAY.minusMonths(4),
				TODAY.minusMonths(1), Instant.now());
		when(membershipRepository.findAllByMemberIdOrderByEndsOnDesc(member.getId())).thenReturn(List.of(ended));
		when(membershipRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		service.grantFor(order);

		assertThat(saved().getStartsOn()).isEqualTo(TODAY);
	}

	/** Granting is idempotent per order: confirming a payment twice adds nothing. */
	@Test
	void shouldGrantAtMostOncePerOrder() {
		when(membershipRepository.existsByOrderId(order.getId())).thenReturn(true);

		service.grantFor(order);

		verify(membershipRepository, never()).save(any());
	}

	@Test
	void shouldDoNothingForAnOrderWithNoMemberYet() {
		order.setMember(null);

		service.grantFor(order);

		verify(membershipRepository, never()).save(any());
	}

	@Test
	void shouldGrantByHandForMoneyThatArrivedOutsideTheShop() {
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
		when(membershipRepository.findAllByMemberIdOrderByEndsOnDesc(member.getId())).thenReturn(List.of());
		when(membershipRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		var response = service.grantManually(member.getId(), "  Nakit ödeme  ", 2);

		assertThat(response.label()).isEqualTo("Nakit ödeme");
		assertThat(response.endsOn()).isEqualTo(TODAY.plusMonths(2));
		assertThat(response.active()).isTrue();
		assertThat(response.daysLeft()).isEqualTo(java.time.temporal.ChronoUnit.DAYS.between(TODAY, TODAY.plusMonths(2)));
	}
}
