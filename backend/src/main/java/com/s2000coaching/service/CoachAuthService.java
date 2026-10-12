package com.s2000coaching.service;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.domain.CoachSession;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.CoachSessionRepository;
import com.s2000coaching.web.dto.CoachResponse;
import com.s2000coaching.web.dto.CoachSessionResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-in for the management area. Replaces the shared {@code ADMIN_TOKEN}: real accounts,
 * bcrypt-hashed passwords, and per-login sessions that can be revoked one at a time.
 */
@Service
public class CoachAuthService {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final CoachAccountRepository coachRepository;
	private final CoachSessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final Duration sessionTtl;
	private final Duration lockoutDuration;

	public CoachAuthService(CoachAccountRepository coachRepository,
			CoachSessionRepository sessionRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.session-ttl}") Duration sessionTtl,
			@Value("${app.admin.lockout-duration}") Duration lockoutDuration) {
		this.coachRepository = coachRepository;
		this.sessionRepository = sessionRepository;
		this.passwordEncoder = passwordEncoder;
		this.sessionTtl = sessionTtl;
		this.lockoutDuration = lockoutDuration;
	}

	/**
	 * A wrong address and a wrong password answer the same way, so the endpoint cannot be
	 * used to find out which accounts exist.
	 *
	 * <p>{@code noRollbackFor} is what makes the lockout real: the failed-attempt counter is
	 * raised and then this method throws, so with the default rollback rule the count would be
	 * undone on the way out and the account would never lock, however many wrong passwords
	 * were tried.
	 */
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public CoachSessionResponse login(String rawEmail, String password) {
		Instant now = Instant.now();
		Optional<CoachAccount> found = coachRepository.findByEmail(normalizeEmail(rawEmail));
		if (found.isEmpty()) {
			throw new UnauthorizedException("E-posta veya şifre hatalı.");
		}

		CoachAccount coach = found.get();
		if (coach.isLockedAt(now)) {
			throw new UnauthorizedException(
					"Çok fazla hatalı deneme yapıldı. Bir süre sonra tekrar deneyin.");
		}

		if (!passwordEncoder.matches(password, coach.getPasswordHash())) {
			coach.registerFailedAttempt(now, lockoutDuration);
			throw new UnauthorizedException("E-posta veya şifre hatalı.");
		}

		coach.registerSuccessfulLogin(now);

		String token = generateToken();
		CoachSession session = new CoachSession(
				UUID.randomUUID(), coach, Sha256.hex(token), now, now.plus(sessionTtl));
		sessionRepository.save(session);

		return new CoachSessionResponse(token, session.getExpiresAt(), CoachResponse.from(coach));
	}

	@Transactional
	public CoachAccount requireCoach(String authorizationHeader) {
		String token = extractToken(authorizationHeader);
		CoachSession session = sessionRepository.findByTokenHash(Sha256.hex(token))
				.orElseThrow(() -> new UnauthorizedException("Oturum geçersiz. Tekrar giriş yapın."));

		if (session.isExpiredAt(Instant.now())) {
			sessionRepository.delete(session);
			throw new UnauthorizedException("Oturum süresi doldu. Tekrar giriş yapın.");
		}
		return session.getCoach();
	}

	@Transactional
	public void logout(String authorizationHeader) {
		String token = extractToken(authorizationHeader);
		sessionRepository.findByTokenHash(Sha256.hex(token)).ifPresent(sessionRepository::delete);
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String extractToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new UnauthorizedException("Oturum bulunamadı. Tekrar giriş yapın.");
		}
		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		if (token.isEmpty()) {
			throw new UnauthorizedException("Oturum bulunamadı. Tekrar giriş yapın.");
		}
		return token;
	}

	private String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
