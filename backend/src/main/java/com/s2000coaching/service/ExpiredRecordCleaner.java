package com.s2000coaching.service;

import com.s2000coaching.repository.CoachSessionRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes sessions that have expired and verification codes nobody can use any more. Both
 * tables only ever grow otherwise: a member who signs in weekly leaves a dead session behind
 * every time, and every code ever mailed stays for good.
 *
 * <p>Expired rows are never read — {@code requireMember} and {@code requireCoach} reject on
 * the stored expiry, and a code is matched by its newest unconsumed row — so removing them
 * changes no behaviour. The one exception is the verification rate limit, which counts the
 * codes issued in the last hour; see {@link #codeRetention}.
 */
@Service
public class ExpiredRecordCleaner {

	private static final Logger log = LoggerFactory.getLogger(ExpiredRecordCleaner.class);

	/**
	 * The rate limit counts rows, so deleting a code the limit would still have counted hands
	 * the caller a fresh allowance. Anything below the limit's own window is refused here
	 * rather than quietly weakening it.
	 */
	private static final Duration MIN_CODE_RETENTION = Duration.ofHours(2);

	private final MemberSessionRepository memberSessionRepository;
	private final CoachSessionRepository coachSessionRepository;
	private final MemberVerificationCodeRepository codeRepository;
	private final Duration codeRetention;

	public ExpiredRecordCleaner(MemberSessionRepository memberSessionRepository,
			CoachSessionRepository coachSessionRepository,
			MemberVerificationCodeRepository codeRepository,
			@Value("${app.cleanup.code-retention}") Duration codeRetention) {
		this.memberSessionRepository = memberSessionRepository;
		this.coachSessionRepository = coachSessionRepository;
		this.codeRepository = codeRepository;
		this.codeRetention = codeRetention.compareTo(MIN_CODE_RETENTION) < 0
				? MIN_CODE_RETENTION
				: codeRetention;
		if (!this.codeRetention.equals(codeRetention)) {
			log.warn("app.cleanup.code-retention ({}) kod isteme sınırının penceresinden kısa; "
					+ "{} olarak uygulanıyor.", codeRetention, MIN_CODE_RETENTION);
		}
	}

	@Scheduled(cron = "${app.cleanup.cron}")
	@Transactional
	public void purge() {
		Instant now = Instant.now();
		int memberSessions = memberSessionRepository.deleteByExpiresAtBefore(now);
		int coachSessions = coachSessionRepository.deleteByExpiresAtBefore(now);
		int codes = codeRepository.deleteByCreatedAtBefore(now.minus(codeRetention));

		if (memberSessions + coachSessions + codes > 0) {
			log.info("Temizlik: {} üye oturumu, {} koç oturumu, {} doğrulama kodu silindi",
					memberSessions, coachSessions, codes);
		}
	}
}
