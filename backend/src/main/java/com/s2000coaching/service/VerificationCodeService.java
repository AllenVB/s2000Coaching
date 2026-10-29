package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberVerificationCode;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.TooManyRequestsException;
import com.s2000coaching.mail.MailDelivery;
import com.s2000coaching.mail.VerificationMailSender;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Issues and verifies the one-time codes mailed to members. */
@Service
public class VerificationCodeService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int CODE_DIGITS = 6;
	private static final int MAX_ATTEMPTS = 5;

	private final MemberVerificationCodeRepository codeRepository;
	private final VerificationMailSender mailSender;
	private final Duration codeTtl;
	private final Duration resendCooldown;
	private final int hourlyLimit;

	public VerificationCodeService(MemberVerificationCodeRepository codeRepository,
			VerificationMailSender mailSender,
			@Value("${app.verification.code-ttl}") Duration codeTtl,
			@Value("${app.verification.resend-cooldown}") Duration resendCooldown,
			@Value("${app.verification.hourly-limit}") int hourlyLimit) {
		this.codeRepository = codeRepository;
		this.mailSender = mailSender;
		this.codeTtl = codeTtl;
		this.resendCooldown = resendCooldown;
		this.hourlyLimit = hourlyLimit;
	}

	/**
	 * Invalidates any outstanding code of the same purpose, stores the digest of a fresh one
	 * and mails the plain code to the member. The plain code is never returned or persisted;
	 * what comes back is only whether the mail went out.
	 *
	 * <p>Rate limited per member, not per caller: anyone who knows an address could otherwise
	 * ask for codes in a loop, and every one of them lands in that member's inbox. The limit
	 * counts codes already issued rather than keeping its own counter, so it survives a
	 * restart and cannot drift from what was actually sent.
	 */
	@Transactional
	public MailDelivery issueAndSend(Member member, VerificationPurpose purpose) {
		Instant now = Instant.now();
		enforceRateLimit(member.getId(), now);
		codeRepository.findAllByMemberIdAndConsumedAtIsNull(member.getId()).stream()
				.filter(code -> code.getPurpose() == purpose)
				.forEach(code -> code.consume(now));

		String code = generateCode();
		codeRepository.save(new MemberVerificationCode(
				UUID.randomUUID(), member, purpose, Sha256.hex(code), now, now.plus(codeTtl)));

		return mailSender.sendVerificationCode(member, code, purpose);
	}

	private void enforceRateLimit(UUID memberId, Instant now) {
		codeRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId).ifPresent(last -> {
			Duration since = Duration.between(last.getCreatedAt(), now);
			if (since.compareTo(resendCooldown) < 0) {
				long wait = Math.max(1, resendCooldown.minus(since).toSeconds());
				throw new TooManyRequestsException(
						"Yeni kod istemek için " + wait + " saniye bekleyin.");
			}
		});

		if (codeRepository.countByMemberIdAndCreatedAtAfter(memberId, now.minus(Duration.ofHours(1)))
				>= hourlyLimit) {
			throw new TooManyRequestsException(
					"Bu adres için saatlik kod sınırına ulaşıldı. Bir saat sonra tekrar deneyin.");
		}
	}

	/**
	 * Checks {@code rawCode} against the member's newest unconsumed code of this purpose and
	 * consumes it on a match. Runs in its own transaction so that the failed-attempt counter
	 * survives the {@link BadRequestException} the caller raises for a non-OK outcome.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public VerificationOutcome tryConsume(UUID memberId, VerificationPurpose purpose, String rawCode) {
		Optional<MemberVerificationCode> found = codeRepository
				.findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(memberId, purpose);
		if (found.isEmpty()) {
			return VerificationOutcome.NO_ACTIVE_CODE;
		}

		MemberVerificationCode code = found.get();
		Instant now = Instant.now();
		if (code.isExpiredAt(now)) {
			return VerificationOutcome.EXPIRED;
		}
		if (code.getAttemptCount() >= MAX_ATTEMPTS) {
			return VerificationOutcome.TOO_MANY_ATTEMPTS;
		}
		if (!Sha256.matches(code.getCodeHash(), Sha256.hex(rawCode.trim()))) {
			code.registerFailedAttempt();
			return VerificationOutcome.MISMATCH;
		}

		code.consume(now);
		return VerificationOutcome.OK;
	}

	/** Translates a non-OK outcome into the message shown to the user. */
	public static void assertVerified(VerificationOutcome outcome) {
		switch (outcome) {
			case OK -> {
			}
			case NO_ACTIVE_CODE -> throw new BadRequestException(
					"Bu hesap için bekleyen bir onay kodu yok. Yeni kod isteyin.");
			case EXPIRED -> throw new BadRequestException("Onay kodunun süresi doldu. Yeni kod isteyin.");
			case TOO_MANY_ATTEMPTS -> throw new BadRequestException(
					"Çok fazla hatalı deneme yapıldı. Yeni kod isteyin.");
			case MISMATCH -> throw new BadRequestException("Onay kodu hatalı.");
		}
	}

	private String generateCode() {
		StringBuilder sb = new StringBuilder(CODE_DIGITS);
		for (int i = 0; i < CODE_DIGITS; i++) {
			sb.append(RANDOM.nextInt(10));
		}
		return sb.toString();
	}
}
