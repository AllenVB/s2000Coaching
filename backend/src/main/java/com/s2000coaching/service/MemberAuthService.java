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
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.EmailNotVerifiedException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.web.dto.RegisterMemberRequest;
import com.s2000coaching.web.dto.RegistrationResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private static final int MAX_FAILED_LOGINS = 5;
	private static final String WRONG_CREDENTIALS = "E-posta veya şifre hatalı.";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final MemberRepository memberRepository;
	private final MemberIdentity memberIdentity;
	private final MemberSessionRepository sessionRepository;
	private final VerificationCodeService verificationCodeService;
	private final PasswordEncoder passwordEncoder;
	private final Duration sessionTtl;
	private final Duration lockoutDuration;
	private final boolean verifyEmail;
	/**
	 * A real bcrypt hash of nothing in particular, checked against when the address is unknown,
	 * so a wrong address costs as much time as a wrong password and the two cannot be told apart
	 * by how long the answer takes.
	 */
	private final String decoyHash;

	public MemberAuthService(MemberRepository memberRepository,
			MemberIdentity memberIdentity,
			MemberSessionRepository sessionRepository,
			VerificationCodeService verificationCodeService,
			PasswordEncoder passwordEncoder,
			@Value("${app.member-session.ttl}") Duration sessionTtl,
			@Value("${app.member.lockout-duration}") Duration lockoutDuration,
			@Value("${app.registration.verify-email}") boolean verifyEmail) {
		this.memberRepository = memberRepository;
		this.memberIdentity = memberIdentity;
		this.sessionRepository = sessionRepository;
		this.verificationCodeService = verificationCodeService;
		this.passwordEncoder = passwordEncoder;
		this.sessionTtl = sessionTtl;
		this.lockoutDuration = lockoutDuration;
		this.verifyEmail = verifyEmail;
		this.decoyHash = passwordEncoder.encode("not-a-real-password");
	}

	/**
	 * Opens an account from the sign-up form. With e-mail confirmation on (the default) the
	 * account waits for the code that was just mailed and cannot sign in until it is entered;
	 * with it off (development, where no mail server exists) the member is signed in at once.
	 *
	 * <p>Orders placed earlier under the same address are deliberately not attached here: nobody
	 * has proved the address is theirs, and a purchase history is not for whoever types it first.
	 */
	@Transactional
	public RegistrationResponse register(RegisterMemberRequest request) {
		String email = MemberService.normalizeEmail(request.email());
		memberIdentity.requireFreeEmail(email);

		Instant now = Instant.now();
		String fullName = (request.firstName().trim() + " " + request.lastName().trim()).replaceAll("\\s+", " ");
		Member member = new Member(UUID.randomUUID(), email, fullName, "", request.heightCm(),
				request.weightKg(), request.sportsBackground().trim(),
				verifyEmail ? MemberStatus.PENDING_VERIFICATION : MemberStatus.ACTIVE, now);
		member.setGender(request.gender());
		member.setAge(request.age());
		member.setPasswordHash(passwordEncoder.encode(request.password()));
		memberIdentity.assignNumber(member);
		Member saved = memberRepository.save(member);

		if (verifyEmail) {
			verificationCodeService.issueAndSend(saved, VerificationPurpose.REGISTRATION);
			return new RegistrationResponse(true, null);
		}
		return new RegistrationResponse(false, openSession(saved, now));
	}

	/**
	 * A wrong address and a wrong password answer the same way, and so does a member who has no
	 * password yet — the page tells them about the e-mail code in its own text instead.
	 *
	 * <p>{@code noRollbackFor} is what makes the lockout real: the failed-attempt counter is
	 * raised and then this method throws, so with the default rollback rule the count would be
	 * undone on the way out and the account would never lock.
	 */
	@Transactional(noRollbackFor = { UnauthorizedException.class, EmailNotVerifiedException.class })
	public MemberSessionResponse login(String rawEmail, String password) {
		Instant now = Instant.now();
		Optional<Member> found = memberRepository.findByEmail(MemberService.normalizeEmail(rawEmail));
		if (found.isEmpty()) {
			passwordEncoder.matches(password, decoyHash);
			throw new UnauthorizedException(WRONG_CREDENTIALS);
		}

		Member member = found.get();
		if (member.isLoginLockedAt(now)) {
			throw new UnauthorizedException("Çok fazla hatalı deneme yapıldı. Bir süre sonra tekrar deneyin.");
		}

		boolean matches = passwordEncoder.matches(password, member.hasPassword() ? member.getPasswordHash() : decoyHash)
				&& member.hasPassword();
		if (!matches) {
			member.registerFailedLogin(now, lockoutDuration, MAX_FAILED_LOGINS);
			throw new UnauthorizedException(WRONG_CREDENTIALS);
		}

		if (!member.isActive()) {
			// Right password, address never confirmed: the page asks for the code, and requests a
			// fresh one itself — issuing it here would make this method throw from inside the
			// transaction it needs to commit.
			throw new EmailNotVerifiedException("E-posta adresiniz henüz doğrulanmadı. Size gönderilen kodu girin.");
		}

		member.registerSuccessfulLogin();
		return openSession(member, now);
	}

	/**
	 * The e-mailed code proves the address, and with it the member picks a new password. Every
	 * session the member had ends — if the old password was known to someone else, so were those
	 * sessions — and a pending account is confirmed by the same code.
	 */
	@Transactional
	public MemberSessionResponse resetPassword(String rawEmail, String code, String newPassword) {
		Member member = memberRepository.findByEmail(MemberService.normalizeEmail(rawEmail))
				.orElseThrow(() -> new BadRequestException("E-posta adresi veya kod hatalı."));
		VerificationPurpose purpose = member.isActive()
				? VerificationPurpose.LOGIN
				: VerificationPurpose.REGISTRATION;
		VerificationCodeService.assertVerified(verificationCodeService.tryConsume(member.getId(), purpose, code));

		Instant now = Instant.now();
		if (!member.isActive()) {
			member.activate(now);
		}
		member.setPasswordHash(passwordEncoder.encode(newPassword));
		member.registerSuccessfulLogin();
		sessionRepository.deleteAllByMemberId(member.getId());
		return openSession(member, now);
	}

	/**
	 * Sets or changes the member's own password. When one exists the current one must be given,
	 * and a wrong guess counts toward the same lockout as a wrong sign-in: a stolen session
	 * should not be a free place to try passwords. The member's other sessions end.
	 */
	@Transactional(noRollbackFor = BadRequestException.class)
	public void changePassword(UUID memberId, String authorizationHeader, String currentPassword,
			String newPassword) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new UnauthorizedException("Oturum geçersiz. Tekrar giriş yapın."));
		Instant now = Instant.now();
		if (member.hasPassword()) {
			if (member.isLoginLockedAt(now)) {
				throw new BadRequestException("Çok fazla hatalı deneme yapıldı. Bir süre sonra tekrar deneyin.");
			}
			if (currentPassword == null || !passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
				member.registerFailedLogin(now, lockoutDuration, MAX_FAILED_LOGINS);
				throw new BadRequestException("Mevcut şifre hatalı.");
			}
		}
		member.setPasswordHash(passwordEncoder.encode(newPassword));
		member.registerSuccessfulLogin();
		sessionRepository.deleteAllByMemberIdAndTokenHashNot(memberId, Sha256.hex(extractToken(authorizationHeader)));
	}

	/** The coach choosing a password for a member. Their sessions end; they sign in with the new one. */
	@Transactional
	public void setPasswordAsCoach(UUID memberId, String newPassword) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new NotFoundException("Üye bulunamadı: " + memberId));
		member.setPasswordHash(passwordEncoder.encode(newPassword));
		member.registerSuccessfulLogin();
		sessionRepository.deleteAllByMemberId(memberId);
	}

	private MemberSessionResponse openSession(Member member, Instant now) {
		String token = generateToken();
		MemberSession session = new MemberSession(
				UUID.randomUUID(), member, Sha256.hex(token), now, now.plus(sessionTtl));
		sessionRepository.save(session);
		return new MemberSessionResponse(token, session.getExpiresAt(), MemberResponse.from(member));
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

		return openSession(member, now);
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
