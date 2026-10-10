package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberVerificationCode;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
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

	public VerificationCodeService(MemberVerificationCodeRepository codeRepository,
			VerificationMailSender mailSender,
			@Value("${app.verification.code-ttl}") Duration codeTtl) {
		this.codeRepository = codeRepository;
		this.mailSender = mailSender;
		this.codeTtl = codeTtl;
	}

	/**
	 * Invalidates any outstanding code of the same purpose, stores the digest of a fresh one
	 * and mails the plain code to the member. The plain code is never returned or persisted.
	 */
	@Transactional
	public void issueAndSend(Member member, VerificationPurpose purpose) {
		Instant now = Instant.now();
		codeRepository.findAllByMemberIdAndConsumedAtIsNull(member.getId()).stream()
				.filter(code -> code.getPurpose() == purpose)
				.forEach(code -> code.consume(now));

		String code = generateCode();
		codeRepository.save(new MemberVerificationCode(
				UUID.randomUUID(), member, purpose, Sha256.hex(code), now, now.plus(codeTtl)));

		mailSender.sendVerificationCode(member, code, purpose);
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
