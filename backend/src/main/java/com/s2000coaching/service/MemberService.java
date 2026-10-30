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
import com.s2000coaching.mail.MailDelivery;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.CreateMemberResponse;
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
	private final MemberIdentity memberIdentity;
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
	private final List<MemberDataEraser> erasers;

	public MemberService(MemberRepository memberRepository,
			MemberIdentity memberIdentity,
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
			@Lazy ProgressPhotoService progressPhotoService,
			// Erasers depend on this class (to look members up), so they are resolved lazily too.
			@Lazy List<MemberDataEraser> erasers) {
		this.memberRepository = memberRepository;
		this.memberIdentity = memberIdentity;
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
		this.erasers = erasers;
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
		String email = normalizeEmail(request.email());
		if (!email.equals(member.getEmail())) {
			memberIdentity.requireFreeEmail(email);
			member.setEmail(email);
		}
		member.setFullName(request.fullName().trim());
		member.setGender(request.gender());
		member.setAge(request.age());
		member.setPhone(blankToEmpty(request.phone()));
		member.setHeightCm(request.heightCm());
		member.setWeightKg(request.weightKg());
		member.setSportsBackground(request.sportsBackground().trim());
		return MemberResponse.from(member);
	}

	/**
	 * The member editing themselves. Narrower than {@link #updateMember}: see
	 * {@link UpdateOwnProfileRequest} for which fields are withheld.
	 */
	@Transactional
	public MemberResponse updateOwnProfile(UUID memberId, UpdateOwnProfileRequest request) {
		Member member = getOrThrow(memberId);
		member.setAge(request.age());
		member.setPhone(blankToEmpty(request.phone()));
		member.setHeightCm(request.heightCm());
		member.setWeightKg(request.weightKg());
		member.setSportsBackground(request.sportsBackground().trim());
		return MemberResponse.from(member);
	}

	private static String blankToEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * Creates the member as {@link MemberStatus#PENDING_VERIFICATION} and mails a registration
	 * code. The member only becomes {@code ACTIVE} once that code is confirmed — either here
	 * by the admin or by the member on the member page.
	 */
	@Transactional
	public CreateMemberResponse createMember(CreateMemberRequest request) {
		String email = normalizeEmail(request.email());
		memberIdentity.requireFreeEmail(email);

		Member member = new Member(
				UUID.randomUUID(),
				email,
				request.fullName().trim(),
				blankToEmpty(request.phone()),
				request.heightCm(),
				request.weightKg(),
				request.sportsBackground().trim(),
				MemberStatus.PENDING_VERIFICATION,
				Instant.now());

		member.setGender(request.gender());
		member.setAge(request.age());
		memberIdentity.assignNumber(member);
		Member saved = memberRepository.save(member);
		// Any order already placed with this address becomes part of the new member's history.
		adminOrderService.linkOrdersTo(saved);
		MailDelivery delivery = verificationCodeService.issueAndSend(saved, VerificationPurpose.REGISTRATION);
		return new CreateMemberResponse(MemberResponse.from(saved), delivery);
	}

	/** @return whether the new code was mailed, so the coach can be told when it was not */
	@Transactional
	public MailDelivery resendRegistrationCode(UUID memberId) {
		Member member = getOrThrow(memberId);
		if (member.isActive()) {
			throw new ConflictException("Üye zaten onaylanmış: " + member.getEmail());
		}
		return verificationCodeService.issueAndSend(member, VerificationPurpose.REGISTRATION);
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

	/** The member switching reminder e-mails on or off for themselves. */
	@Transactional
	public MemberResponse setRemindersEnabled(UUID memberId, boolean enabled) {
		Member member = getOrThrow(memberId);
		member.setRemindersEnabled(enabled);
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
		// Everything else member-owned that registered itself (food log, check-ins, ...).
		erasers.forEach(eraser -> eraser.eraseAllOf(memberId));
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
