package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberGender;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.EmailNotVerifiedException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.mail.MailDelivery;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.web.dto.RegisterMemberRequest;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Sign-up, password sign-in, the lockout, and the ways a password is set or reset. */
@ExtendWith(MockitoExtension.class)
class MemberPasswordAuthTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private MemberSessionRepository sessionRepository;
	@Mock
	private com.s2000coaching.repository.CoachAccountRepository coachRepository;
	@Mock
	private VerificationCodeService verificationCodeService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

	private MemberAuthService service(boolean verifyEmail) {
		return new MemberAuthService(memberRepository, new MemberIdentity(memberRepository, coachRepository), sessionRepository, verificationCodeService,
				passwordEncoder, Duration.ofDays(14), Duration.ofMinutes(15), verifyEmail);
	}

	private MemberAuthService service;

	@BeforeEach
	void setUp() {
		service = service(true);
	}

	private static RegisterMemberRequest signUp(String email) {
		return new RegisterMemberRequest(email, "gizli-parola", "  Ahmet ", " Yılmaz  ", 27, MemberGender.MALE,
				182, new BigDecimal("84.50"), "5 yıl fitness", true);
	}

	private Member memberWithPassword(String password, MemberStatus status) {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "", 182,
				new BigDecimal("84.50"), "fitness", status, Instant.now());
		member.setPasswordHash(passwordEncoder.encode(password));
		return member;
	}

	@Nested
	class SigningUp {

		@Test
		void shouldCreateAPendingAccountAndMailTheCodeWhenTheAddressMustBeConfirmed() {
			when(memberRepository.existsByEmail("yeni@example.com")).thenReturn(false);
			when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(verificationCodeService.issueAndSend(any(), eq(VerificationPurpose.REGISTRATION)))
					.thenReturn(MailDelivery.SENT);

			var response = service.register(signUp("  Yeni@Example.COM "));

			assertThat(response.verificationRequired()).isTrue();
			assertThat(response.session()).isNull();
			var saved = ArgumentCaptor.forClass(Member.class);
			verify(memberRepository).save(saved.capture());
			Member member = saved.getValue();
			assertThat(member.getEmail()).isEqualTo("yeni@example.com");
			assertThat(member.getFullName()).isEqualTo("Ahmet Yılmaz");
			assertThat(member.getGender()).isEqualTo(MemberGender.MALE);
			assertThat(member.getAge()).isEqualTo(27);
			assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING_VERIFICATION);
			// Only the bcrypt hash is stored, and it is the hash of what was typed.
			assertThat(member.getPasswordHash()).isNotEqualTo("gizli-parola");
			assertThat(passwordEncoder.matches("gizli-parola", member.getPasswordHash())).isTrue();
			verify(sessionRepository, never()).save(any());
		}

		@Test
		void shouldSignTheMemberInAtOnceWhenNoConfirmationIsRequired() {
			service = service(false);
			when(memberRepository.existsByEmail(anyString())).thenReturn(false);
			when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = service.register(signUp("yeni@example.com"));

			assertThat(response.verificationRequired()).isFalse();
			assertThat(response.session().token()).isNotBlank();
			assertThat(response.session().member().status()).isEqualTo(MemberStatus.ACTIVE);
			verify(verificationCodeService, never()).issueAndSend(any(), any());
		}

		@Test
		void shouldRefuseAnAddressThatAlreadyHasAnAccount() {
			when(memberRepository.existsByEmail("var@example.com")).thenReturn(true);

			assertThatThrownBy(() -> service.register(signUp("var@example.com")))
					.isInstanceOf(ConflictException.class)
					.hasMessageContaining("zaten bir hesap");
			verify(memberRepository, never()).save(any());
		}
	}

	@Nested
	class SigningIn {

		@Test
		void shouldOpenASessionForTheRightPassword() {
			Member member = memberWithPassword("gizli-parola", MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));

			var session = service.login(" Sporcu@Example.com ", "gizli-parola");

			assertThat(session.token()).isNotBlank();
			assertThat(session.member().email()).isEqualTo("sporcu@example.com");
			verify(sessionRepository).save(any());
		}

		@Test
		void shouldAnswerAnUnknownAddressAndAWrongPasswordTheSameWay() {
			Member member = memberWithPassword("gizli-parola", MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(memberRepository.findByEmail("yok@example.com")).thenReturn(Optional.empty());

			String unknown = org.junit.jupiter.api.Assertions.assertThrows(UnauthorizedException.class,
					() -> service.login("yok@example.com", "gizli-parola")).getMessage();
			String wrong = org.junit.jupiter.api.Assertions.assertThrows(UnauthorizedException.class,
					() -> service.login("sporcu@example.com", "yanlis-parola")).getMessage();

			assertThat(unknown).isEqualTo(wrong).isEqualTo("E-posta veya şifre hatalı.");
		}

		@Test
		void shouldNotLetAMemberWithoutAPasswordInAndNotSayWhy() {
			Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet", "", 182,
					new BigDecimal("84.50"), "fitness", MemberStatus.ACTIVE, Instant.now());
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));

			assertThatThrownBy(() -> service.login("sporcu@example.com", "herhangi-bir-sey"))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessage("E-posta veya şifre hatalı.");
		}

		@Test
		void shouldLockAfterFiveWrongPasswordsAndRefuseEvenTheRightOneUntilItLifts() {
			Member member = memberWithPassword("gizli-parola", MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));

			for (int attempt = 0; attempt < 5; attempt++) {
				assertThatThrownBy(() -> service.login("sporcu@example.com", "yanlis"))
						.isInstanceOf(UnauthorizedException.class);
			}

			assertThat(member.isLoginLockedAt(Instant.now())).isTrue();
			assertThatThrownBy(() -> service.login("sporcu@example.com", "gizli-parola"))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessageContaining("Çok fazla hatalı deneme");
			verify(sessionRepository, never()).save(any());
		}

		@Test
		void shouldForgiveEarlierMistakesAfterASuccessfulSignIn() {
			Member member = memberWithPassword("gizli-parola", MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			for (int attempt = 0; attempt < 4; attempt++) {
				assertThatThrownBy(() -> service.login("sporcu@example.com", "yanlis"))
						.isInstanceOf(UnauthorizedException.class);
			}

			service.login("sporcu@example.com", "gizli-parola");
			assertThatThrownBy(() -> service.login("sporcu@example.com", "yanlis"))
					.isInstanceOf(UnauthorizedException.class);

			// Four mistakes, a success, one more mistake: nowhere near five in a row.
			assertThat(member.isLoginLockedAt(Instant.now())).isFalse();
		}

		@Test
		void shouldSendAnUnconfirmedMemberToTheCodeStepOnlyAfterTheRightPassword() {
			Member member = memberWithPassword("gizli-parola", MemberStatus.PENDING_VERIFICATION);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));

			assertThatThrownBy(() -> service.login("sporcu@example.com", "gizli-parola"))
					.isInstanceOf(EmailNotVerifiedException.class);
			// A wrong password must not reveal that the account exists and is still unconfirmed.
			assertThatThrownBy(() -> service.login("sporcu@example.com", "yanlis"))
					.isInstanceOf(UnauthorizedException.class);
			verify(sessionRepository, never()).save(any());
		}
	}

	@Nested
	class ChoosingAPassword {

		@Test
		void shouldResetWithTheMailedCodeEndOldSessionsAndSignIn() {
			Member member = memberWithPassword("eski-parola", MemberStatus.ACTIVE);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "123456"))
					.thenReturn(VerificationOutcome.OK);

			var session = service.resetPassword("sporcu@example.com", "123456", "yeni-parola-1");

			assertThat(passwordEncoder.matches("yeni-parola-1", member.getPasswordHash())).isTrue();
			assertThat(session.token()).isNotBlank();
			verify(sessionRepository).deleteAllByMemberId(member.getId());
		}

		@Test
		void shouldConfirmAPendingAccountWithTheSameCode() {
			Member member = memberWithPassword("eski-parola", MemberStatus.PENDING_VERIFICATION);
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.REGISTRATION, "123456"))
					.thenReturn(VerificationOutcome.OK);

			service.resetPassword("sporcu@example.com", "123456", "yeni-parola-1");

			assertThat(member.isActive()).isTrue();
		}

		@Test
		void shouldNotChangeAnythingForAWrongCode() {
			Member member = memberWithPassword("eski-parola", MemberStatus.ACTIVE);
			String before = member.getPasswordHash();
			when(memberRepository.findByEmail("sporcu@example.com")).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.LOGIN, "000000"))
					.thenReturn(VerificationOutcome.MISMATCH);

			assertThatThrownBy(() -> service.resetPassword("sporcu@example.com", "000000", "yeni-parola-1"))
					.isInstanceOf(BadRequestException.class);

			assertThat(member.getPasswordHash()).isEqualTo(before);
			verify(sessionRepository, never()).deleteAllByMemberId(any());
		}

		@Test
		void shouldRequireTheCurrentPasswordToChangeItAndKeepTheCurrentSession() {
			Member member = memberWithPassword("eski-parola", MemberStatus.ACTIVE);
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			service.changePassword(member.getId(), "Bearer tok", "eski-parola", "yeni-parola-1");

			assertThat(passwordEncoder.matches("yeni-parola-1", member.getPasswordHash())).isTrue();
			verify(sessionRepository).deleteAllByMemberIdAndTokenHashNot(member.getId(), Sha256.hex("tok"));
		}

		@Test
		void shouldCountAWrongCurrentPasswordTowardTheLockout() {
			Member member = memberWithPassword("eski-parola", MemberStatus.ACTIVE);
			String before = member.getPasswordHash();
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			for (int attempt = 0; attempt < 5; attempt++) {
				assertThatThrownBy(() -> service.changePassword(member.getId(), "Bearer tok", "yanlis", "yeni-parola-1"))
						.isInstanceOf(BadRequestException.class);
			}

			assertThat(member.getPasswordHash()).isEqualTo(before);
			assertThat(member.isLoginLockedAt(Instant.now())).isTrue();
		}

		@Test
		void shouldLetAMemberWithoutAPasswordSetTheFirstOneWithoutAskingForTheOld() {
			Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet", "", 182,
					new BigDecimal("84.50"), "fitness", MemberStatus.ACTIVE, Instant.now());
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			service.changePassword(member.getId(), "Bearer tok", null, "ilk-parola-1");

			assertThat(passwordEncoder.matches("ilk-parola-1", member.getPasswordHash())).isTrue();
		}

		@Test
		void shouldLetTheCoachSetAPasswordAndEndTheMembersSessions() {
			Member member = memberWithPassword("eski-parola", MemberStatus.ACTIVE);
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			service.setPasswordAsCoach(member.getId(), "koc-verdi-1");

			assertThat(passwordEncoder.matches("koc-verdi-1", member.getPasswordHash())).isTrue();
			verify(sessionRepository).deleteAllByMemberId(member.getId());
		}
	}
}
