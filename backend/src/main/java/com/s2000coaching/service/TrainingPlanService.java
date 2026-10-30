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

	/** A programme has at least one and at most seven training days. */
	static final int MIN_DAYS = 1;
	static final int MAX_DAYS = 7;

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

	/**
	 * A new template starts with its first day ("Gün 1", empty); the coach adds the other days one
	 * at a time, up to seven. {@code blank} is kept for older callers and changes nothing: a plan
	 * always has at least one day.
	 */
	@Transactional
	public TrainingPlanResponse createTemplate(UpsertTrainingPlanRequest request, boolean blank) {
		TrainingPlan plan = planRepository.save(new TrainingPlan(UUID.randomUUID(), null,
				request.name().trim(), blankToNull(request.note()), false, Instant.now()));
		seedFirstDay(plan);
		return TrainingPlanMapper.toResponse(plan);
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
	 * Creates a plan for the member and makes it the active one — assigning a plan the member
	 * cannot see would be a surprising default. It is a copy of the template when one is given,
	 * otherwise it has its first day, "Gün 1", ready for exercises; {@code blank} changes nothing.
	 */
	@Transactional
	public TrainingPlanResponse createForMember(UUID memberId, UpsertTrainingPlanRequest request,
			UUID templateId, boolean blank) {
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
		} else {
			seedFirstDay(plan);
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
		if (plan.getDays().size() >= MAX_DAYS) {
			throw new BadRequestException("Bir programda en fazla %d antrenman günü olabilir.".formatted(MAX_DAYS));
		}
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
		if (plan.getDays().size() <= MIN_DAYS) {
			throw new BadRequestException("Bir programda en az %d antrenman günü olmalı; son gün silinemez."
					.formatted(MIN_DAYS));
		}
		deleteLogsForDays(List.of(day.getId()));
		plan.getDays().remove(day);
		plan.setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(plan);
	}

	/* ---------- exercises inside a day ---------- */

	@Transactional
	public TrainingPlanResponse addExercise(UUID dayId, UpsertPlanExerciseRequest request) {
		TrainingPlanDay day = getDayOrThrow(dayId);
		Exercise exercise = exerciseService.resolve(request.exerciseId(), request.exerciseName());
		int nextOrder = day.getExercises().stream()
				.mapToInt(TrainingPlanExercise::getDisplayOrder).max().orElse(0) + 1;

		Prescription prescription = prescriptionOf(request);
		TrainingPlanExercise planExercise = new TrainingPlanExercise(UUID.randomUUID(), day, exercise,
				nextOrder, prescription.setCount(), prescription.reps(), request.targetWeightKg(),
				request.restSeconds(), blankToNull(request.note()));
		planExercise.setSetReps(prescription.setReps());
		day.getExercises().add(planExercise);
		day.getPlan().setUpdatedAt(Instant.now());
		return TrainingPlanMapper.toResponse(day.getPlan());
	}

	@Transactional
	public TrainingPlanResponse updateExercise(UUID planExerciseId, UpsertPlanExerciseRequest request) {
		TrainingPlanExercise planExercise = getPlanExerciseOrThrow(planExerciseId);
		planExercise.setExercise(exerciseService.resolve(request.exerciseId(), request.exerciseName()));
		Prescription prescription = prescriptionOf(request);
		planExercise.setSetCount(prescription.setCount());
		planExercise.setReps(prescription.reps());
		planExercise.setSetReps(prescription.setReps());
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
				copy.setSetReps(sourceExercise.getSetReps());
				day.getExercises().add(copy);
			}
		}
	}

	/** What a new plan starts from: its first day, empty and ready for exercises. */
	private void seedFirstDay(TrainingPlan plan) {
		plan.getDays().add(new TrainingPlanDay(UUID.randomUUID(), plan, 1, "Gün 1", null));
	}

	/** The sets and reps as stored: how many sets, the text shown for them, and the per-set list. */
	private record Prescription(int setCount, String reps, String setReps) {
	}

	/**
	 * Per-set targets win when any is given: they must be filled from set 1 without a gap
	 * (a blank set 2 with a set 3 would silently renumber), and set count and reps are derived
	 * from them so everything that reads those two keeps working. Otherwise the older
	 * count-and-free-text shape is required.
	 */
	private static Prescription prescriptionOf(UpsertPlanExerciseRequest request) {
		List<String> entered = request.setReps() == null
				? List.of()
				: request.setReps().stream().map(value -> value == null ? "" : value.trim()).toList();

		if (entered.stream().allMatch(String::isEmpty)) {
			if (request.setCount() == null || request.reps() == null || request.reps().isBlank()) {
				throw new BadRequestException("En az Set 1'in tekrarını girin.");
			}
			return new Prescription(request.setCount(), request.reps().trim(), null);
		}

		int filled = 0;
		while (filled < entered.size() && !entered.get(filled).isEmpty()) {
			filled++;
		}
		for (int index = filled; index < entered.size(); index++) {
			if (!entered.get(index).isEmpty()) {
				throw new BadRequestException(
						"Set " + (filled + 1) + " boş ama sonraki setler dolu; setleri baştan sırayla doldurun.");
			}
		}
		List<String> sets = entered.subList(0, filled);
		if (sets.stream().anyMatch(value -> value.contains("|"))) {
			throw new BadRequestException("Tekrar alanında | karakteri kullanılamaz.");
		}
		return new Prescription(filled, String.join("-", sets), String.join("|", sets));
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
