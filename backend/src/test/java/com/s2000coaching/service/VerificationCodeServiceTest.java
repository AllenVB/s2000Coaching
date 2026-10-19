package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MemberVerificationCode;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.TooManyRequestsException;
import com.s2000coaching.mail.VerificationMailSender;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

	private static final Duration TTL = Duration.ofMinutes(15);
	private static final Duration COOLDOWN = Duration.ofSeconds(60);
	private static final int HOURLY_LIMIT = 5;

	@Mock
	private MemberVerificationCodeRepository codeRepository;

	@Mock
	private VerificationMailSender mailSender;

	private VerificationCodeService verificationCodeService;
	private Member member;

	@BeforeEach
	void setUp() {
		verificationCodeService = new VerificationCodeService(codeRepository, mailSender, TTL, COOLDOWN,
				HOURLY_LIMIT);
		member = newMember();
	}

	private static Member newMember() {
		return new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
	}

	private MemberVerificationCode storedCode(String rawCode, VerificationPurpose purpose, Instant expiresAt) {
		return new MemberVerificationCode(UUID.randomUUID(), member, purpose, Sha256.hex(rawCode),
				Instant.now(), expiresAt);
	}

	@Nested
	class Issuing {

		@Test
		void shouldMailASixDigitCodeAndPersistOnlyItsHash() {
			when(codeRepository.findAllByMemberIdAndConsumedAtIsNull(member.getId())).thenReturn(List.of());

			verificationCodeService.issueAndSend(member, VerificationPurpose.REGISTRATION);

			var mailedCode = ArgumentCaptor.forClass(String.class);
			verify(mailSender).sendVerificationCode(eq(member), mailedCode.capture(),
					eq(VerificationPurpose.REGISTRATION));
			assertThat(mailedCode.getValue()).matches("\\d{6}");

			var saved = ArgumentCaptor.forClass(MemberVerificationCode.class);
			verify(codeRepository).save(saved.capture());
			assertThat(saved.getValue().getCodeHash())
					.isNotEqualTo(mailedCode.getValue())
					.isEqualTo(Sha256.hex(mailedCode.getValue()));
			assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now());
		}

		@Test
		void shouldInvalidateOutstandingCodesOfTheSamePurposeOnly() {
			var samePurpose = storedCode("111111", VerificationPurpose.REGISTRATION, Instant.now().plus(TTL));
			var otherPurpose = storedCode("222222", VerificationPurpose.LOGIN, Instant.now().plus(TTL));
			when(codeRepository.findAllByMemberIdAndConsumedAtIsNull(member.getId()))
					.thenReturn(List.of(samePurpose, otherPurpose));

			verificationCodeService.issueAndSend(member, VerificationPurpose.REGISTRATION);

			assertThat(samePurpose.isConsumed()).isTrue();
			assertThat(otherPurpose.isConsumed()).isFalse();
		}

		@Test
		void shouldRefuseASecondCodeWithinTheCooldown() {
			when(codeRepository.findFirstByMemberIdOrderByCreatedAtDesc(member.getId()))
					.thenReturn(Optional.of(codeIssuedSecondsAgo(10)));

			assertThatThrownBy(() -> verificationCodeService.issueAndSend(member, VerificationPurpose.LOGIN))
					.isInstanceOf(TooManyRequestsException.class)
					.hasMessageContaining("saniye bekleyin");

			verify(mailSender, never()).sendVerificationCode(any(), any(), any());
			verify(codeRepository, never()).save(any());
		}

		@Test
		void shouldAllowANewCodeOnceTheCooldownHasPassed() {
			when(codeRepository.findFirstByMemberIdOrderByCreatedAtDesc(member.getId()))
					.thenReturn(Optional.of(codeIssuedSecondsAgo(90)));
			when(codeRepository.findAllByMemberIdAndConsumedAtIsNull(member.getId())).thenReturn(List.of());

			verificationCodeService.issueAndSend(member, VerificationPurpose.LOGIN);

			verify(mailSender).sendVerificationCode(eq(member), any(), eq(VerificationPurpose.LOGIN));
		}

		/** The cooldown alone would still allow a code a minute, all night. */
		@Test
		void shouldRefuseWhenTheHourlyLimitIsReached() {
			when(codeRepository.findFirstByMemberIdOrderByCreatedAtDesc(member.getId()))
					.thenReturn(Optional.of(codeIssuedSecondsAgo(300)));
			when(codeRepository.countByMemberIdAndCreatedAtAfter(eq(member.getId()), any()))
					.thenReturn((long) HOURLY_LIMIT);

			assertThatThrownBy(() -> verificationCodeService.issueAndSend(member, VerificationPurpose.LOGIN))
					.isInstanceOf(TooManyRequestsException.class)
					.hasMessageContaining("saatlik kod sınırına");

			verify(mailSender, never()).sendVerificationCode(any(), any(), any());
		}

		private MemberVerificationCode codeIssuedSecondsAgo(long seconds) {
			Instant createdAt = Instant.now().minusSeconds(seconds);
			return new MemberVerificationCode(UUID.randomUUID(), member, VerificationPurpose.LOGIN,
					Sha256.hex("123456"), createdAt, createdAt.plus(TTL));
		}
	}

	@Nested
	class Consuming {

		@Test
		void shouldConsumeAMatchingCode() {
			var code = storedCode("123456", VerificationPurpose.REGISTRATION, Instant.now().plus(TTL));
			when(codeRepository.findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
					member.getId(), VerificationPurpose.REGISTRATION)).thenReturn(Optional.of(code));

			var outcome = verificationCodeService.tryConsume(
					member.getId(), VerificationPurpose.REGISTRATION, " 123456 ");

			assertThat(outcome).isEqualTo(VerificationOutcome.OK);
			assertThat(code.isConsumed()).isTrue();
		}

		@Test
		void shouldCountFailedAttemptsAndStopAtTheLimit() {
			var code = storedCode("123456", VerificationPurpose.LOGIN, Instant.now().plus(TTL));
			when(codeRepository.findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
					member.getId(), VerificationPurpose.LOGIN)).thenReturn(Optional.of(code));

			for (int i = 0; i < 5; i++) {
				assertThat(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "000000"))
						.isEqualTo(VerificationOutcome.MISMATCH);
			}

			assertThat(code.getAttemptCount()).isEqualTo(5);
			assertThat(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "123456"))
					.isEqualTo(VerificationOutcome.TOO_MANY_ATTEMPTS);
			assertThat(code.isConsumed()).isFalse();
		}

		@Test
		void shouldRejectAnExpiredCode() {
			var code = storedCode("123456", VerificationPurpose.LOGIN, Instant.now().minusSeconds(1));
			when(codeRepository.findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
					member.getId(), VerificationPurpose.LOGIN)).thenReturn(Optional.of(code));

			assertThat(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "123456"))
					.isEqualTo(VerificationOutcome.EXPIRED);
			assertThat(code.isConsumed()).isFalse();
		}

		@Test
		void shouldReportWhenThereIsNoOutstandingCode() {
			when(codeRepository.findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
					member.getId(), VerificationPurpose.LOGIN)).thenReturn(Optional.empty());

			assertThat(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "123456"))
					.isEqualTo(VerificationOutcome.NO_ACTIVE_CODE);
			verify(codeRepository, never()).save(any());
		}
	}

	@Nested
	class OutcomeMessages {

		@Test
		void shouldPassForOk() {
			VerificationCodeService.assertVerified(VerificationOutcome.OK);
		}

		@Test
		void shouldRaiseAMessageForEveryFailure() {
			assertThatThrownBy(() -> VerificationCodeService.assertVerified(VerificationOutcome.MISMATCH))
					.isInstanceOf(BadRequestException.class)
					.hasMessage("Onay kodu hatalı.");
			assertThatThrownBy(() -> VerificationCodeService.assertVerified(VerificationOutcome.EXPIRED))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("süresi doldu");
			assertThatThrownBy(() -> VerificationCodeService.assertVerified(VerificationOutcome.TOO_MANY_ATTEMPTS))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("hatalı deneme");
			assertThatThrownBy(() -> VerificationCodeService.assertVerified(VerificationOutcome.NO_ACTIVE_CODE))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("bekleyen bir onay kodu yok");
		}
	}
}
