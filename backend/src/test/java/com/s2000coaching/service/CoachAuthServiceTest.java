package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.domain.CoachSession;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.CoachSessionRepository;
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
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class CoachAuthServiceTest {

	private static final String PASSWORD = "dogru-sifre";

	@Mock
	private CoachAccountRepository coachRepository;

	@Mock
	private CoachSessionRepository sessionRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private CoachAuthService coachAuthService;
	private CoachAccount coach;

	@BeforeEach
	void setUp() {
		coachAuthService = new CoachAuthService(coachRepository, sessionRepository, passwordEncoder,
				Duration.ofHours(12), Duration.ofMinutes(15));
		coach = new CoachAccount(UUID.randomUUID(), "koc@example.com", passwordEncoder.encode(PASSWORD),
				"Koç", Instant.now());
	}

	@Nested
	class LoggingIn {

		@Test
		void shouldOpenASessionForTheRightPassword() {
			when(coachRepository.findByEmail("koc@example.com")).thenReturn(Optional.of(coach));

			var response = coachAuthService.login("  Koc@Example.COM ", PASSWORD);

			assertThat(response.token()).isNotBlank();
			assertThat(response.coach().email()).isEqualTo("koc@example.com");
			assertThat(coach.getLastLoginAt()).isNotNull();

			var saved = ArgumentCaptor.forClass(CoachSession.class);
			verify(sessionRepository).save(saved.capture());
			// Only the digest is stored, never the token itself.
			assertThat(saved.getValue().getTokenHash()).isNotEqualTo(response.token()).hasSize(64);
		}

		@Test
		void shouldAnswerTheSameWayForAWrongAddressAndAWrongPassword() {
			when(coachRepository.findByEmail("yok@example.com")).thenReturn(Optional.empty());
			when(coachRepository.findByEmail("koc@example.com")).thenReturn(Optional.of(coach));

			assertThatThrownBy(() -> coachAuthService.login("yok@example.com", PASSWORD))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessage("E-posta veya şifre hatalı.");
			assertThatThrownBy(() -> coachAuthService.login("koc@example.com", "yanlis"))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessage("E-posta veya şifre hatalı.");

			verify(sessionRepository, never()).save(any());
		}

		@Test
		void shouldLockTheAccountAfterRepeatedWrongPasswords() {
			when(coachRepository.findByEmail("koc@example.com")).thenReturn(Optional.of(coach));

			for (int i = 0; i < CoachAccount.MAX_FAILED_ATTEMPTS; i++) {
				assertThatThrownBy(() -> coachAuthService.login("koc@example.com", "yanlis"))
						.isInstanceOf(UnauthorizedException.class);
			}

			assertThat(coach.isLockedAt(Instant.now())).isTrue();
			// Even the right password is refused while the lock holds.
			assertThatThrownBy(() -> coachAuthService.login("koc@example.com", PASSWORD))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessageContaining("Çok fazla hatalı deneme");
			verify(sessionRepository, never()).save(any());
		}

		/**
		 * The tests above work on an entity in memory, so they stay green even when the
		 * counter never reaches the database. The lockout only works for real if the
		 * transaction that raised the count commits despite the rejection that follows it.
		 */
		@Test
		void shouldKeepTheFailureCountWhenTheLoginIsRejected() throws NoSuchMethodException {
			Transactional tx = CoachAuthService.class
					.getMethod("login", String.class, String.class)
					.getAnnotation(Transactional.class);

			assertThat(tx).isNotNull();
			assertThat(tx.noRollbackFor()).contains(UnauthorizedException.class);
		}

		@Test
		void shouldClearTheFailureCountOnASuccessfulLogin() {
			when(coachRepository.findByEmail("koc@example.com")).thenReturn(Optional.of(coach));
			assertThatThrownBy(() -> coachAuthService.login("koc@example.com", "yanlis"))
					.isInstanceOf(UnauthorizedException.class);
			assertThat(coach.getFailedAttempts()).isEqualTo(1);

			coachAuthService.login("koc@example.com", PASSWORD);

			assertThat(coach.getFailedAttempts()).isZero();
			assertThat(coach.getLockedUntil()).isNull();
		}
	}

	@Nested
	class Sessions {

		@Test
		void shouldResolveAValidBearerToken() {
			var session = new CoachSession(UUID.randomUUID(), coach, Sha256.hex("tok"), Instant.now(),
					Instant.now().plusSeconds(60));
			when(sessionRepository.findByTokenHash(Sha256.hex("tok"))).thenReturn(Optional.of(session));

			assertThat(coachAuthService.requireCoach("Bearer tok")).isSameAs(coach);
		}

		@Test
		void shouldRejectAMissingOrMalformedHeader() {
			assertThatThrownBy(() -> coachAuthService.requireCoach(null))
					.isInstanceOf(UnauthorizedException.class);
			assertThatThrownBy(() -> coachAuthService.requireCoach("tok"))
					.isInstanceOf(UnauthorizedException.class);
			assertThatThrownBy(() -> coachAuthService.requireCoach("Bearer   "))
					.isInstanceOf(UnauthorizedException.class);
		}

		@Test
		void shouldDropAnExpiredSession() {
			var session = new CoachSession(UUID.randomUUID(), coach, Sha256.hex("tok"),
					Instant.now().minusSeconds(120), Instant.now().minusSeconds(60));
			when(sessionRepository.findByTokenHash(Sha256.hex("tok"))).thenReturn(Optional.of(session));

			assertThatThrownBy(() -> coachAuthService.requireCoach("Bearer tok"))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessageContaining("süresi doldu");
			verify(sessionRepository).delete(session);
		}

		@Test
		void shouldDeleteTheSessionOnLogout() {
			var session = new CoachSession(UUID.randomUUID(), coach, Sha256.hex("tok"), Instant.now(),
					Instant.now().plusSeconds(60));
			when(sessionRepository.findByTokenHash(Sha256.hex("tok"))).thenReturn(Optional.of(session));

			coachAuthService.logout("Bearer tok");

			verify(sessionRepository).delete(session);
		}
	}

	@Nested
	class ChangingThePassword {

		@Test
		void shouldStoreTheNewHashAndEndEverySession() {
			String oldHash = coach.getPasswordHash();

			coachAuthService.changePassword(coach, PASSWORD, "yeni-uzun-sifre");

			assertThat(coach.getPasswordHash()).isNotEqualTo(oldHash);
			assertThat(passwordEncoder.matches("yeni-uzun-sifre", coach.getPasswordHash())).isTrue();
			verify(coachRepository).save(coach);
			verify(sessionRepository).deleteAllByCoachId(coach.getId());
		}

		@Test
		void shouldRefuseAWrongCurrentPassword() {
			String oldHash = coach.getPasswordHash();

			assertThatThrownBy(() -> coachAuthService.changePassword(coach, "yanlis", "yeni-uzun-sifre"))
					.isInstanceOf(UnauthorizedException.class)
					.hasMessageContaining("Mevcut şifre");

			assertThat(coach.getPasswordHash()).isEqualTo(oldHash);
			verify(sessionRepository, never()).deleteAllByCoachId(any());
		}

		/** Otherwise the coach believes the password changed while the old one still works. */
		@Test
		void shouldRefuseTheSamePasswordAgain() {
			assertThatThrownBy(() -> coachAuthService.changePassword(coach, PASSWORD, PASSWORD))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("eskisiyle aynı");

			verify(coachRepository, never()).save(any());
			verify(sessionRepository, never()).deleteAllByCoachId(any());
		}
	}
}
