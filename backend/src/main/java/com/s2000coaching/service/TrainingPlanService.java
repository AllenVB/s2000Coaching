package com.s2000coaching.service;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.SetLogRepository;
import com.s2000coaching.repository.TrainingPlanDayRepository;
import com.s2000coaching.repository.TrainingPlanExerciseRepository;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.UpsertPlanDayRequest;
import com.s2000coaching.web.dto.UpsertPlanExerciseRequest;
import com.s2000coaching.web.dto.UpsertTrainingPlanRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plan authoring for the coach: templates, member plans, and the days and exercises inside
 * them. Assigning a template deep-copies it, so later edits to either side stay independent.
 */
@Service
public class TrainingPlanService {

	private final TrainingPlanRepository planRepository;
	private final TrainingPlanDayRepository dayRepository;
	private final TrainingPlanExerciseRepository planExerciseRepository;
	private final WorkoutLogRepository workoutLogRepository;
	private final SetLogRepository setLogRepository;
	private final ExerciseService exerciseService;
	private final MemberService memberService;

	public TrainingPlanService(TrainingPlanRepository planRepository,
			TrainingPlanDayRepository dayRepository,
			TrainingPlanExerciseRepository planExerciseRepository,
			WorkoutLogRepository workoutLogRepository,
			SetLogRepository setLogRepository,
			ExerciseService exerciseService,
			MemberService memberService) {
		this.planRepository = planRepository;
		this.dayRepository = dayRepository;
		this.planExerciseRepository = planExerciseRepository;
		this.workoutLogRepository = workoutLogRepository;
		this.setLogRepository = setLogRepository;
		this.exerciseService = exerciseService;
		this.memberService = memberService;
	}

	/* ---------- templates ---------- */

	@Transactional(readOnly = true)
	public List<TrainingPlanResponse> listTemplates() {
		return planRepository.findAllByMemberIsNullOrderByNameAsc().stream()
				.map(TrainingPlanMapper::toResponse)
				.toList();
	}

	@Transactional
	public TrainingPlanResponse createTemplate(UpsertTrainingPlanRequest request) {
		TrainingPlan plan = new TrainingPlan(UUID.randomUUID(), null, request.name().trim(),
				blankToNull(request.note()), false, Instant.now());
		return TrainingPlanMapper.toResponse(planRepository.save(plan));
	}

	/* ---------- member plans ---------- */

	@Transactional(readOnly = true)
	public List<TrainingPlanResponse> listForMember(UUID memberId) {
		memberService.requireMember(memberId);
		return planRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.map(TrainingPlanMapper::toResponse)
				.toList();
	}

	/**
	 * Creates a plan for the member, either empty or as a copy of a template, and makes it the
	 * active one — assigning a plan the member cannot see would be a surprising default.
	 */
	@Transactional
	public TrainingPlanResponse createForMember(UUID memberId, UpsertTrainingPlanRequest request,
			UUID templateId) {
		Member member = memberService.requireMember(memberId);
		Instant now = Instant.now();

		// Entities carry assigned UUIDs, so save() merges and hands back a *different*,
		// managed instance. Everything after this point must use that one, or the changes
		// are made to a detached copy and quietly lost.
		TrainingPlan plan = planRepository.save(new TrainingPlan(UUID.randomUUID(), member,
				request.name().trim(), blankToNull(request.note()), true, now));

		if (templateId != null) {
			TrainingPlan template = getPlanOrThrow(templateId);
			if (!template.isTemplate()) {
				throw new BadRequestException("Yalnızca şablonlar kopyalanabilir.");
			}
			copyInto(plan, template);
		}

		deactivateOtherPlans(memberId, plan.getId());
		return TrainingPlanMapper.toResponse(plan);
	}

	@Transactional
	public TrainingPlanResponse updatePlan(UUID planId, UpsertTrainingPlanRequest request) {
		TrainingPlan plan = getPlanOrThrow(planId);
		plan.setName(request.name().trim());
		plan.setNote(blankToNull(request.note()));
		plan.setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(plan);
	}

	@Transactional
	public TrainingPlanResponse activatePlan(UUID planId) {
		TrainingPlan plan = getPlanOrThrow(planId);
		if (plan.isTemplate()) {
			throw new BadRequestException("Şablonlar aktifleştirilemez; önce bir üyeye atayın.");
		}
		plan.setActive(true);
		plan.setUpdatedAt(Instant.now());
		deactivateOtherPlans(plan.getMember().getId(), plan.getId());
		return TrainingPlanMapper.toResponse(plan);
	}

	@Transactional
	public void deletePlan(UUID planId) {
		TrainingPlan plan = getPlanOrThrow(planId);
		// Sessions reference this plan's days, so they go first or the foreign key blocks it.
		deleteLogsForDays(plan.getDays().stream().map(TrainingPlanDay::getId).toList());
		planRepository.delete(plan);
	}

	/* ---------- days ---------- */

	@Transactional
	public TrainingPlanResponse addDay(UUID planId, UpsertPlanDayRequest request) {
		TrainingPlan plan = getPlanOrThrow(planId);
		int nextOrder = plan.getDays().stream().mapToInt(TrainingPlanDay::getDayOrder).max().orElse(0) + 1;
		TrainingPlanDay day = new TrainingPlanDay(UUID.randomUUID(), plan, nextOrder, request.name().trim(),
				blankToNull(request.note()));
		// Cascaded from the managed plan; an explicit save() here would merge a second
		// instance with the same id and fail the flush.
		plan.getDays().add(day);
		plan.setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(plan);
	}

	@Transactional
	public TrainingPlanResponse updateDay(UUID dayId, UpsertPlanDayRequest request) {
		TrainingPlanDay day = getDayOrThrow(dayId);
		day.setName(request.name().trim());
		day.setNote(blankToNull(request.note()));
		day.getPlan().setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(day.getPlan());
	}

	@Transactional
	public TrainingPlanResponse deleteDay(UUID dayId) {
		TrainingPlanDay day = getDayOrThrow(dayId);
		TrainingPlan plan = day.getPlan();
		deleteLogsForDays(List.of(day.getId()));
		plan.getDays().remove(day);
		plan.setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(plan);
	}

	/* ---------- exercises inside a day ---------- */

	@Transactional
	public TrainingPlanResponse addExercise(UUID dayId, UpsertPlanExerciseRequest request) {
		TrainingPlanDay day = getDayOrThrow(dayId);
		Exercise exercise = exerciseService.requireExercise(request.exerciseId());
		int nextOrder = day.getExercises().stream()
				.mapToInt(TrainingPlanExercise::getDisplayOrder).max().orElse(0) + 1;

		TrainingPlanExercise planExercise = new TrainingPlanExercise(UUID.randomUUID(), day, exercise,
				nextOrder, request.setCount(), request.reps().trim(), request.targetWeightKg(),
				request.restSeconds(), blankToNull(request.note()));
		day.getExercises().add(planExercise);
		day.getPlan().setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(day.getPlan());
	}

	@Transactional
	public TrainingPlanResponse updateExercise(UUID planExerciseId, UpsertPlanExerciseRequest request) {
		TrainingPlanExercise planExercise = getPlanExerciseOrThrow(planExerciseId);
		planExercise.setExercise(exerciseService.requireExercise(request.exerciseId()));
		planExercise.setSetCount(request.setCount());
		planExercise.setReps(request.reps().trim());
		planExercise.setTargetWeightKg(request.targetWeightKg());
		planExercise.setRestSeconds(request.restSeconds());
		planExercise.setNote(blankToNull(request.note()));
		planExercise.getDay().getPlan().setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(planExercise.getDay().getPlan());
	}

	@Transactional
	public TrainingPlanResponse deleteExercise(UUID planExerciseId) {
		TrainingPlanExercise planExercise = getPlanExerciseOrThrow(planExerciseId);
		TrainingPlanDay day = planExercise.getDay();
		// Recorded sets point at this prescription; drop them before it disappears.
		setLogRepository.deleteAllByPlanExerciseId(planExerciseId);
		setLogRepository.flush();
		day.getExercises().remove(planExercise);
		day.getPlan().setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(day.getPlan());
	}

	/* ---------- internals ---------- */

	/** Deep copy: new rows all the way down, so the two plans share nothing afterwards. */
	private void copyInto(TrainingPlan target, TrainingPlan source) {
		for (TrainingPlanDay sourceDay : source.getDays()) {
			TrainingPlanDay day = new TrainingPlanDay(UUID.randomUUID(), target, sourceDay.getDayOrder(),
					sourceDay.getName(), sourceDay.getNote());
			target.getDays().add(day);

			for (TrainingPlanExercise sourceExercise : sourceDay.getExercises()) {
				TrainingPlanExercise copy = new TrainingPlanExercise(UUID.randomUUID(), day,
						sourceExercise.getExercise(), sourceExercise.getDisplayOrder(),
						sourceExercise.getSetCount(), sourceExercise.getReps(),
						sourceExercise.getTargetWeightKg(), sourceExercise.getRestSeconds(),
						sourceExercise.getNote());
				day.getExercises().add(copy);
			}
		}
	}

	private void deleteLogsForDays(List<UUID> dayIds) {
		if (!dayIds.isEmpty()) {
			workoutLogRepository.deleteAllByDayIdIn(dayIds);
			// Force the deletes out before the days themselves are removed in this same flush.
			workoutLogRepository.flush();
		}
	}

	private void deactivateOtherPlans(UUID memberId, UUID keepActiveId) {
		planRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.filter(other -> !other.getId().equals(keepActiveId))
				.forEach(other -> other.setActive(false));
	}

	TrainingPlan getPlanOrThrow(UUID planId) {
		return planRepository.findById(planId)
				.orElseThrow(() -> new NotFoundException("Plan bulunamadı: " + planId));
	}

	TrainingPlanDay getDayOrThrow(UUID dayId) {
		return dayRepository.findById(dayId)
				.orElseThrow(() -> new NotFoundException("Antrenman günü bulunamadı: " + dayId));
	}

	private TrainingPlanExercise getPlanExerciseOrThrow(UUID planExerciseId) {
		return planExerciseRepository.findById(planExerciseId)
				.orElseThrow(() -> new NotFoundException("Plan hareketi bulunamadı: " + planExerciseId));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
