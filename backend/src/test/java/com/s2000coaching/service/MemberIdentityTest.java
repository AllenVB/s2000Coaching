package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberIdentityTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private CoachAccountRepository coachRepository;

	private MemberIdentity identity;

	@BeforeEach
	void setUp() {
		identity = new MemberIdentity(memberRepository, coachRepository);
	}

	private static Member member() {
		return new Member(UUID.randomUUID(), "a@example.com", "Ahmet Yılmaz", "", 180, new BigDecimal("80"),
				"spor", MemberStatus.ACTIVE, Instant.now());
	}

	@Test
	void shouldRefuseAnAddressAMemberAlreadyHas() {
		when(memberRepository.existsByEmail("a@example.com")).thenReturn(true);

		assertThatThrownBy(() -> identity.requireFreeEmail("a@example.com")).isInstanceOf(ConflictException.class);
	}

	@Test
	void shouldRefuseAnAddressACoachHasWithTheSameMessage() {
		org.mockito.Mockito.lenient().when(coachRepository.existsByEmail("koc@example.com")).thenReturn(true);
		org.mockito.Mockito.lenient().when(memberRepository.existsByEmail("a@example.com")).thenReturn(true);

		String coachMessage = org.junit.jupiter.api.Assertions
				.assertThrows(ConflictException.class, () -> identity.requireFreeEmail("koc@example.com")).getMessage();
		String memberMessage = org.junit.jupiter.api.Assertions
				.assertThrows(ConflictException.class, () -> identity.requireFreeEmail("a@example.com")).getMessage();

		assertThat(coachMessage).isEqualTo(memberMessage);
	}

	@Test
	void shouldAcceptAFreeAddress() {
		identity.requireFreeEmail("yeni@example.com");
	}

	@Test
	void shouldGiveAFiveDigitNumber() {
		Member member = member();

		identity.assignNumber(member);

		assertThat(member.getMemberNumber()).isBetween(10_000, 99_999);
	}

	@Test
	void shouldPickAgainWhenTheNumberIsTaken() {
		when(memberRepository.existsByMemberNumber(anyInt())).thenReturn(true, true, false);
		Member member = member();

		identity.assignNumber(member);

		assertThat(member.getMemberNumber()).isBetween(10_000, 99_999);
	}

	@Test
	void shouldGiveUpWhenEveryNumberTriedIsTaken() {
		when(memberRepository.existsByMemberNumber(anyInt())).thenReturn(true);

		assertThatThrownBy(() -> identity.assignNumber(member())).isInstanceOf(ConflictException.class);
	}

	@Test
	void shouldNeverChangeANumberOnceGiven() {
		Member member = member();
		identity.assignNumber(member);

		assertThatThrownBy(() -> member.assignMemberNumber(12345)).isInstanceOf(IllegalStateException.class);
	}
}
