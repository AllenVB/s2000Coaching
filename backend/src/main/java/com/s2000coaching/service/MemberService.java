package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.MemberResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin-side member administration: create, list, confirm and delete members. */
@Service
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberVerificationCodeRepository codeRepository;
	private final MemberSessionRepository sessionRepository;
	private final VerificationCodeService verificationCodeService;

	public MemberService(MemberRepository memberRepository,
			MemberVerificationCodeRepository codeRepository,
			MemberSessionRepository sessionRepository,
			VerificationCodeService verificationCodeService) {
		this.memberRepository = memberRepository;
		this.codeRepository = codeRepository;
		this.sessionRepository = sessionRepository;
		this.verificationCodeService = verificationCodeService;
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> listMembers() {
		return memberRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(MemberResponse::from)
				.toList();
	}

	/**
	 * Creates the member as {@link MemberStatus#PENDING_VERIFICATION} and mails a registration
	 * code. The member only becomes {@code ACTIVE} once that code is confirmed — either here
	 * by the admin or by the member on the member page.
	 */
	@Transactional
	public MemberResponse createMember(CreateMemberRequest request) {
		String email = normalizeEmail(request.email());
		if (memberRepository.existsByEmail(email)) {
			throw new ConflictException("Bu e-posta ile kayıtlı bir üye zaten var: " + email);
		}

		Member member = new Member(
				UUID.randomUUID(),
				email,
				request.fullName().trim(),
				request.phone().trim(),
				request.heightCm(),
				request.weightKg(),
				request.sportsBackground().trim(),
				MemberStatus.PENDING_VERIFICATION,
				Instant.now());

		Member saved = memberRepository.save(member);
		verificationCodeService.issueAndSend(saved, VerificationPurpose.REGISTRATION);
		return MemberResponse.from(saved);
	}

	@Transactional
	public void resendRegistrationCode(UUID memberId) {
		Member member = getOrThrow(memberId);
		if (member.isActive()) {
			throw new ConflictException("Üye zaten onaylanmış: " + member.getEmail());
		}
		verificationCodeService.issueAndSend(member, VerificationPurpose.REGISTRATION);
	}

	@Transactional
	public MemberResponse completeRegistration(UUID memberId, String code) {
		Member member = getOrThrow(memberId);
		if (member.isActive()) {
			throw new ConflictException("Üye zaten onaylanmış: " + member.getEmail());
		}

		VerificationCodeService.assertVerified(
				verificationCodeService.tryConsume(memberId, VerificationPurpose.REGISTRATION, code));
		member.activate(Instant.now());
		return MemberResponse.from(member);
	}

	@Transactional
	public void deleteMember(UUID memberId) {
		Member member = getOrThrow(memberId);
		sessionRepository.deleteAllByMemberId(memberId);
		codeRepository.deleteAllByMemberId(memberId);
		memberRepository.delete(member);
	}

	private Member getOrThrow(UUID memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new NotFoundException("Üye bulunamadı: " + memberId));
	}
}
