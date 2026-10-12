package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CoachNoteRepository;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import com.s2000coaching.repository.MessageRepository;
import com.s2000coaching.repository.NutritionPlanRepository;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.UpdateMemberRequest;
import com.s2000coaching.web.dto.UpdateOwnProfileRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin-side member administration: create, list, confirm and delete members. */
@Service
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberVerificationCodeRepository codeRepository;
	private final MemberSessionRepository sessionRepository;
	private final CoachNoteRepository coachNoteRepository;
	private final MeasurementRepository measurementRepository;
	private final WorkoutLogRepository workoutLogRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final NutritionPlanRepository nutritionPlanRepository;
	private final MessageRepository messageRepository;
	private final AdminOrderService adminOrderService;
	private final VerificationCodeService verificationCodeService;
	private final ProgressPhotoService progressPhotoService;

	public MemberService(MemberRepository memberRepository,
			MemberVerificationCodeRepository codeRepository,
			MemberSessionRepository sessionRepository,
			CoachNoteRepository coachNoteRepository,
			MeasurementRepository measurementRepository,
			WorkoutLogRepository workoutLogRepository,
			TrainingPlanRepository trainingPlanRepository,
			NutritionPlanRepository nutritionPlanRepository,
			MessageRepository messageRepository,
			AdminOrderService adminOrderService,
			VerificationCodeService verificationCodeService,
			// Photos need a member to hang off, so ProgressPhotoService depends on this class;
			// deleting a member has to delete their photos, so this class depends back. @Lazy
			// breaks the cycle at startup rather than duplicating the photo-deletion logic here.
			@Lazy ProgressPhotoService progressPhotoService) {
		this.memberRepository = memberRepository;
		this.codeRepository = codeRepository;
		this.sessionRepository = sessionRepository;
		this.coachNoteRepository = coachNoteRepository;
		this.measurementRepository = measurementRepository;
		this.workoutLogRepository = workoutLogRepository;
		this.trainingPlanRepository = trainingPlanRepository;
		this.nutritionPlanRepository = nutritionPlanRepository;
		this.messageRepository = messageRepository;
		this.adminOrderService = adminOrderService;
		this.verificationCodeService = verificationCodeService;
		this.progressPhotoService = progressPhotoService;
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

	@Transactional(readOnly = true)
	public MemberResponse getMember(UUID memberId) {
		return MemberResponse.from(getOrThrow(memberId));
	}

	@Transactional
	public MemberResponse updateMember(UUID memberId, UpdateMemberRequest request) {
		Member member = getOrThrow(memberId);
		member.setFullName(request.fullName().trim());
		member.setPhone(request.phone().trim());
		member.setHeightCm(request.heightCm());
		member.setWeightKg(request.weightKg());
		member.setSportsBackground(request.sportsBackground().trim());
		return MemberResponse.from(member);
	}

	/**
	 * The member editing themselves. Narrower than {@link #updateMember}: see
	 * {@link UpdateOwnProfileRequest} for which fields are withheld and why.
	 */
	@Transactional
	public MemberResponse updateOwnProfile(UUID memberId, UpdateOwnProfileRequest request) {
		Member member = getOrThrow(memberId);
		member.setFullName(request.fullName().trim());
		member.setPhone(request.phone().trim());
		member.setHeightCm(request.heightCm());
		member.setSportsBackground(request.sportsBackground().trim());
		return MemberResponse.from(member);
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
		// Any order already placed with this address becomes part of the new member's history.
		adminOrderService.linkOrdersTo(saved);
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
		coachNoteRepository.deleteAllByMemberId(memberId);
		measurementRepository.deleteAllByMemberId(memberId);
		// Logs reference plan days, so they go before the plans that own those days.
		workoutLogRepository.deleteAllByMemberId(memberId);
		trainingPlanRepository.deleteAllByMemberId(memberId);
		nutritionPlanRepository.deleteAllByMemberId(memberId);
		messageRepository.deleteAllByMemberId(memberId);
		// Photos carry their files with them: dropping only the rows would leave body photos
		// on disk after the member asked to be removed.
		progressPhotoService.deleteAllOfMember(memberId);
		// Orders are financial records: detach them rather than deleting them with the member.
		adminOrderService.unlinkOrdersOf(memberId);
		memberRepository.delete(member);
	}

	/**
	 * Records the member's explicit consent to health-data processing (KVKK special-category
	 * data). Nothing in the measurement flow writes a row before this is set.
	 */
	@Transactional
	public MemberResponse grantHealthDataConsent(UUID memberId) {
		Member member = getOrThrow(memberId);
		member.grantHealthDataConsent(Instant.now());
		return MemberResponse.from(member);
	}

	/** Shared by the services that hang records off a member (notes, measurements, plans). */
	@Transactional(readOnly = true)
	public Member requireMember(UUID memberId) {
		return getOrThrow(memberId);
	}

	private Member getOrThrow(UUID memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new NotFoundException("Üye bulunamadı: " + memberId));
	}
}
