package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberSession;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.MemberSessionResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passwordless member sign-in. The member asks for a code by e-mail and exchanges it for an
 * opaque bearer token. A member who is still {@code PENDING_VERIFICATION} uses the registration
 * code from their welcome mail, which both activates the account and signs them in.
 */
@Service
public class MemberAuthService {

	private static final Logger log = LoggerFactory.getLogger(MemberAuthService.class);
	private static final String BEARER_PREFIX = "Bearer ";
	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final MemberRepository memberRepository;
	private final MemberSessionRepository sessionRepository;
	private final VerificationCodeService verificationCodeService;
	private final Duration sessionTtl;

	public MemberAuthService(MemberRepository memberRepository,
			MemberSessionRepository sessionRepository,
			VerificationCodeService verificationCodeService,
			@Value("${app.member-session.ttl}") Duration sessionTtl) {
		this.memberRepository = memberRepository;
		this.sessionRepository = sessionRepository;
		this.verificationCodeService = verificationCodeService;
		this.sessionTtl = sessionTtl;
	}

	/**
	 * Mails a fresh code to the address when it belongs to a member. Unknown addresses are
	 * silently ignored so the endpoint cannot be used to enumerate members.
	 */
	@Transactional
	public void requestCode(String rawEmail) {
		String email = MemberService.normalizeEmail(rawEmail);
		Optional<Member> member = memberRepository.findByEmail(email);
		if (member.isEmpty()) {
			log.info("Kayıtlı olmayan bir adres için kod istendi, istek yok sayıldı");
			return;
		}

		Member found = member.get();
		verificationCodeService.issueAndSend(found,
				found.isActive() ? VerificationPurpose.LOGIN : VerificationPurpose.REGISTRATION);
	}

	@Transactional
	public MemberSessionResponse verifyCode(String rawEmail, String code) {
		String email = MemberService.normalizeEmail(rawEmail);
		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new BadRequestException("E-posta adresi veya kod hatalı."));

		VerificationPurpose purpose = member.isActive()
				? VerificationPurpose.LOGIN
				: VerificationPurpose.REGISTRATION;
		VerificationCodeService.assertVerified(verificationCodeService.tryConsume(member.getId(), purpose, code));

		Instant now = Instant.now();
		if (!member.isActive()) {
			member.activate(now);
		}

		String token = generateToken();
		MemberSession session = new MemberSession(
				UUID.randomUUID(), member, Sha256.hex(token), now, now.plus(sessionTtl));
		sessionRepository.save(session);

		return new MemberSessionResponse(token, session.getExpiresAt(), MemberResponse.from(member));
	}

	@Transactional
	public Member requireMember(String authorizationHeader) {
		String token = extractToken(authorizationHeader);
		MemberSession session = sessionRepository.findByTokenHash(Sha256.hex(token))
				.orElseThrow(() -> new UnauthorizedException("Oturum geçersiz. Tekrar giriş yapın."));

		if (session.isExpiredAt(Instant.now())) {
			sessionRepository.delete(session);
			throw new UnauthorizedException("Oturum süresi doldu. Tekrar giriş yapın.");
		}
		return session.getMember();
	}

	@Transactional
	public void logout(String authorizationHeader) {
		String token = extractToken(authorizationHeader);
		sessionRepository.findByTokenHash(Sha256.hex(token)).ifPresent(sessionRepository::delete);
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
