package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.SaveWorkoutLogRequest;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.WorkoutLogResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The member's side of training: read the active plan, record what was actually lifted. */
@Service
public class WorkoutLogService {

	private final WorkoutLogRepository workoutLogRepository;
	private final TrainingPlanRepository planRepository;
	private final TrainingPlanService trainingPlanService;

	public WorkoutLogService(WorkoutLogRepository workoutLogRepository,
			TrainingPlanRepository planRepository,
			TrainingPlanService trainingPlanService) {
		this.workoutLogRepository = workoutLogRepository;
		this.planRepository = planRepository;
		this.trainingPlanService = trainingPlanService;
	}

	/**
	 * The member's active plan with their sessions attached: the latest per day, so each
	 * exercise can show what they did last time next to what is prescribed, and the weight of
	 * every session for the "Antrenman 1–10" strip.
	 */
	@Transactional(readOnly = true)
	public Optional<TrainingPlanResponse> findActivePlan(UUID memberId) {
		return planRepository.findByMemberIdAndActiveTrue(memberId)
				.map(plan -> TrainingPlanMapper.toMemberResponse(plan, logsByDay(memberId, plan)));
	}

	@Transactional(readOnly = true)
	public List<WorkoutLogResponse> listLogs(UUID memberId) {
		return workoutLogRepository.findAllByMemberIdOrderByPerformedOnDesc(memberId).stream()
				.map(WorkoutLogResponse::from)
				.toList();
	}

	/**
	 * Saves a whole session. Re-saving the same day and date replaces its sets rather than
	 * appending, which is what correcting a typo mid-workout should do.
	 */
	@Transactional
	public WorkoutLogResponse save(Member member, SaveWorkoutLogRequest request) {
		TrainingPlanDay day = trainingPlanService.getDayOrThrow(request.dayId());
		TrainingPlan plan = day.getPlan();
		if (plan.getMember() == null || !plan.getMember().getId().equals(member.getId())) {
			// Same answer as a missing day, so plan ids cannot be probed across members.
			throw new NotFoundException("Antrenman günü bulunamadı: " + request.dayId());
		}

		Map<UUID, TrainingPlanExercise> allowed = new HashMap<>();
		day.getExercises().forEach(exercise -> allowed.put(exercise.getId(), exercise));

		Instant now = Instant.now();
		WorkoutLog log = workoutLogRepository
				.findByMemberIdAndDayIdAndPerformedOn(member.getId(), day.getId(), request.performedOn())
				.orElseGet(() -> workoutLogRepository.save(
						new WorkoutLog(UUID.randomUUID(), member, day, request.performedOn(), now)));

		log.setNote(blankToNull(request.note()));
		log.setUpdatedAt(now);
		log.getSets().clear();

		for (SaveWorkoutLogRequest.SetEntry entry : request.sets()) {
			TrainingPlanExercise planExercise = allowed.get(entry.planExerciseId());
			if (planExercise == null) {
				throw new BadRequestException("Bu harekete ait set bu antrenman gününde yok.");
			}
			SetLog set = new SetLog(UUID.randomUUID(), log, planExercise, entry.setNumber(), entry.reps(),
					entry.weightKg());
			set.setRpe(entry.rpe());
			log.getSets().add(set);
		}

		return WorkoutLogResponse.from(log);
	}

	@Transactional
	public void deleteOwn(Member member, UUID workoutLogId) {
		WorkoutLog log = workoutLogRepository.findById(workoutLogId)
				.orElseThrow(() -> new NotFoundException("Antrenman kaydı bulunamadı: " + workoutLogId));
		if (!log.getMember().getId().equals(member.getId())) {
			throw new NotFoundException("Antrenman kaydı bulunamadı: " + workoutLogId);
		}
		workoutLogRepository.delete(log);
	}

	/** Coach-side read of a member's sessions. */
	@Transactional(readOnly = true)
	public List<WorkoutLogResponse> listLogsAsCoach(UUID memberId) {
		return listLogs(memberId);
	}

	/** Every session of each day of the plan, newest first. */
	private Map<UUID, List<WorkoutLog>> logsByDay(UUID memberId, TrainingPlan plan) {
		Map<UUID, List<WorkoutLog>> logs = new HashMap<>();
		for (TrainingPlanDay day : plan.getDays()) {
			logs.put(day.getId(),
					workoutLogRepository.findAllByMemberIdAndDayIdOrderByPerformedOnDesc(memberId, day.getId()));
		}
		return logs;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
