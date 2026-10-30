package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.MemberRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * What makes one member distinguishable from every other account: an address nobody else uses,
 * and a five-digit number.
 *
 * <p>The address is checked against members <em>and</em> the coach accounts, because both sign in
 * through the same form: if the two could share an address, one person's password would open the
 * other's account or the form would not know whom to let in. The message is the same either way, so
 * it does not say which kind of account holds the address.
 *
 * <p>The number is random rather than counting up, so it says nothing about how many members there
 * are or who joined first; unique, because the point is to tell same-named people apart.
 */
@Component
public class MemberIdentity {

	static final int LOWEST = 10_000;
	static final int HIGHEST = 99_999;
	private static final int ATTEMPTS = 50;

	private final MemberRepository memberRepository;
	private final CoachAccountRepository coachRepository;
	private final SecureRandom random = new SecureRandom();

	public MemberIdentity(MemberRepository memberRepository, CoachAccountRepository coachRepository) {
		this.memberRepository = memberRepository;
		this.coachRepository = coachRepository;
	}

	/** {@code email} is already normalised (see {@link MemberService#normalizeEmail}). */
	public void requireFreeEmail(String email) {
		if (memberRepository.existsByEmail(email) || coachRepository.existsByEmail(email)) {
			throw new ConflictException("Bu e-posta adresiyle zaten bir hesap var. Giriş yapın; şifrenizi "
					+ "bilmiyorsanız \"Şifremi unuttum\"u kullanın.");
		}
	}

	public void assignNumber(Member member) {
		for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
			int candidate = LOWEST + random.nextInt(HIGHEST - LOWEST + 1);
			if (!memberRepository.existsByMemberNumber(candidate)) {
				member.assignMemberNumber(candidate);
				return;
			}
		}
		// Fifty misses in a row means the five-digit space is nearly full: say so, do not loop.
		throw new ConflictException("Yeni üye numarası üretilemedi. Lütfen daha sonra tekrar deneyin.");
	}
}
