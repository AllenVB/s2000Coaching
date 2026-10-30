package com.s2000coaching.service;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.web.dto.AccountSessionResponse;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The one sign-in form for everybody: an address and a password, and the server works out
 * whether the person is the coach or a member. The page therefore needs no "management" switch.
 *
 * <p>Each side still does its own sign-in — lockout, session, timing and all — this only picks
 * which one. A coach whose password matches is signed in as the coach. Anyone else goes to the
 * member sign-in, except an address that belongs to a coach and to no member, which goes to the
 * coach sign-in so that its failed attempts and lockout are counted there. An address nobody has
 * takes the member path, whose decoy hash keeps "unknown address" indistinguishable from "wrong
 * password" by timing.
 *
 * <p>Not transactional on purpose: the two services commit their own failed-attempt counters
 * (see their {@code noRollbackFor}), and wrapping them here would roll those back again.
 */
@Service
public class AccountAuthService {

	private final CoachAccountRepository coachRepository;
	private final MemberRepository memberRepository;
	private final CoachAuthService coachAuthService;
	private final MemberAuthService memberAuthService;
	private final PasswordEncoder passwordEncoder;

	public AccountAuthService(CoachAccountRepository coachRepository, MemberRepository memberRepository,
			CoachAuthService coachAuthService, MemberAuthService memberAuthService,
			PasswordEncoder passwordEncoder) {
		this.coachRepository = coachRepository;
		this.memberRepository = memberRepository;
		this.coachAuthService = coachAuthService;
		this.memberAuthService = memberAuthService;
		this.passwordEncoder = passwordEncoder;
	}

	public AccountSessionResponse login(String rawEmail, String password) {
		String email = MemberService.normalizeEmail(rawEmail);
		Optional<CoachAccount> coach = coachRepository.findByEmail(email);

		if (coach.isPresent()) {
			boolean usable = !coach.get().isLockedAt(Instant.now());
			if (usable && passwordEncoder.matches(password, coach.get().getPasswordHash())) {
				return AccountSessionResponse.ofCoach(coachAuthService.login(email, password));
			}
			if (!memberRepository.existsByEmail(email)) {
				// Only a coach has this address: the failure (or the lockout message) is theirs.
				return AccountSessionResponse.ofCoach(coachAuthService.login(email, password));
			}
		}
		return AccountSessionResponse.ofMember(memberAuthService.login(email, password));
	}
}
