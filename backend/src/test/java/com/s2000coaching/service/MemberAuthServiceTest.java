package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberSession;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberSessionRepository sessionRepository;

	@Mock
	private VerificationCodeService verificationCodeService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

	private MemberAuthService memberAuthService;

	@BeforeEach
	void setUp() {
		memberAuthService = new MemberAuthService(memberRepository, sessionRepository, verificationCodeService,
				passwordEncoder, Duration.ofDays(14), Duration.ofMinutes(15), true);
	}

	private static Member member(MemberStatus status) {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		if (status == MemberStatus.ACTIVE) {
			member.activate(Instant.now());
		}
		return member;
	}

	@Nested
	class RequestingACode {

		@Test
		void shouldMailALoginCodeToAnActiveMember() {
			Member member = member(MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));

			memberAuthService.requestCode(" Sporcu@Example.COM ");

			verify(verificationCodeService).issueAndSend(member, VerificationPurpose.LOGIN);
		}

		@Test
		void shouldMailTheRegistrationCodeAgainToAPendingMember() {
			Member member = member(MemberStatus.PENDING_VERIFICATION);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));

			memberAuthService.requestCode("sporcu@example.com");

			verify(verificationCodeService).issueAndSend(member, VerificationPurpose.REGISTRATION);
		}

		@Test
		void shouldStaySilentForAnUnknownAddressSoMembersCannotBeEnumerated() {
			when(memberRepository.findByEmail("yok@example.com")).thenReturn(Optional.empty());

			memberAuthService.requestCode("yok@example.com");

			verify(verificationCodeService, never()).issueAndSend(any(), any());
		}
	}

	@Nested
	class VerifyingACode {

		@Test
		void shouldActivateAPendingMemberAndOpenASession() {
			Member member = member(MemberStatus.PENDING_VERIFICATION);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.REGISTRATION, "123456"))
					.thenReturn(VerificationOutcome.OK);

			var response = memberAuthService.verifyCode("Sporcu@Example.com", "123456");

			assertThat(member.isActive()).isTrue();
			assertThat(response.member().status()).isEqualTo(MemberStatus.ACTIVE);
			assertThat(response.token()).isNotBlank();
			assertThat(response.expiresAt()).isAfter(Instant.now());

			var saved = ArgumentCaptor.forClass(MemberSession.class);
			verify(sessionRepository).save(saved.capture());
			assertThat(saved.getValue().getTokenHash())
					.isNotEqualTo(response.token())
					.hasSize(64);
		}

		@Test
		void shouldOpenASessionForAnActiveMemberWithoutTouchingVerifiedAt() {
			Member member = member(MemberStatus.ACTIVE);
			Instant verifiedAt = member.getVerifiedAt();
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "123456"))
					.thenReturn(VerificationOutcome.OK);

			memberAuthService.verifyCode("sporcu@example.com", "123456");

			assertThat(member.getVerifiedAt()).isEqualTo(verifiedAt);
			verify(sessionRepository).save(any());
		}

		@Test
		void shouldNotOpenASessionForAWrongCode() {
			Member member = member(MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "000000"))
					.thenReturn(VerificationOutcome.MISMATCH);

			assertThatThrownBy(() -> memberAuthService.verifyCode("sporcu@example.com", "000000"))
					.isInstanceOf(BadRequestException.class);

			verify(sessionRepository, never()).save(any());
		}

		@Test
		void shouldNotRevealThatAnAddressIsUnknown() {
			when(memberRepository.findByEmail("yok@example.com")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> memberAuthService.verifyCode("yok@example.com", "123456"))
					.isInstanceOf(BadRequestException.class)
					.hasMessage("E-posta adresi veya kod hatalı.");
		}
	}

	@Nested
	class Sessions {

		@Test
		void shouldResolveAValidBearerToken() {
			Member member = member(MemberStatus.ACTIVE);
			var session = new MemberSession(UUID.randomUUID(), member, Sha256.hex("tok"), Instant.now(),
					Instant.now().plusSeconds(60));
			when(sessionRepository.findByTokenHash(Sha256.hex("tok"))).thenReturn(Optional.of(session));

			assertThat(memberAuthService.requireMember("Bearer tok")).isSameAs(member);
		}

		@Test
		void shouldRejectAMissingOrMalformedHeader() {
			assertThatThrownBy(() -> memberAuthService.requireMember(null))
					.isInstanceOf(UnauthorizedException.class);
			assertThatThrownBy(() -> memberAuthService.requireMember("tok"))
					.isInstanceOf(UnauthorizedException.class);
			assertThatThrownBy(() -> memberAuthService.requireMember("Bearer   "))
					.isInstanceOf(UnauthorizedException.class);
		}

		@Test
		void shouldDropAnExpiredSession() {
			Member member = member(MemberStatus.ACTIVE);
			var session = new MemberSession(UUID.randomUUID(), member, Sha256.hex("tok"),
					Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));
			when(sessionRepository.findByTokenHash(Sha256.hex("tok"))).thenReturn(Optional.of(session));

			assertThatThrownBy(() -> memberAuthService.requireMember("Bearer tok"))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessageContaining("süresi doldu");

			verify(sessionRepository).delete(session);
		}

		@Test
		void shouldDeleteTheSessionOnLogout() {
			Member member = member(MemberStatus.ACTIVE);
			var session = new MemberSession(UUID.randomUUID(), member, Sha256.hex("tok"), Instant.now(),
					Instant.now().plusSeconds(60));
			when(sessionRepository.findByTokenHash(Sha256.hex("tok"))).thenReturn(Optional.of(session));

			memberAuthService.logout("Bearer tok");

			verify(sessionRepository).delete(session);
		}
	}
}
