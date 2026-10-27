package com.s2000coaching.service;

import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.web.dto.ExerciseResponse;
import com.s2000coaching.web.dto.PlanDayResponse;
import com.s2000coaching.web.dto.PlanExerciseResponse;
import com.s2000coaching.web.dto.SetLogResponse;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.WeightEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds plan responses. The coach gets the plan as written; the member additionally gets what
 * they did last time, which is the number that actually drives the next session.
 */
final class TrainingPlanMapper {

	/** The strip shows ten sessions ("Antrenman 1–10"), so no more are sent. */
	static final int HISTORY_SESSIONS = 10;

	private TrainingPlanMapper() {
	}

	/** The plan as written, with no training history attached. */
	static TrainingPlanResponse toResponse(TrainingPlan plan) {
		return toResponse(plan, Map.of());
	}

	/**
	 * The member's view: each day carries their latest session, and each exercise their weight
	 * history across every session of that day.
	 *
	 * @param logsNewestFirstByDayId the member's {@link WorkoutLog}s per plan-day id, newest first
	 */
	static TrainingPlanResponse toMemberResponse(TrainingPlan plan,
			Map<UUID, List<WorkoutLog>> logsNewestFirstByDayId) {
		return new TrainingPlanResponse(
				plan.getId(),
				plan.getName(),
				plan.getNote(),
				plan.isTemplate(),
				plan.isActive(),
				plan.getUpdatedAt(),
				plan.getDays().stream()
						.map(day -> {
							List<WorkoutLog> logs = logsNewestFirstByDayId.getOrDefault(day.getId(), List.of());
							return toDayResponse(day, logs.isEmpty() ? null : logs.get(0), logs);
						})
						.toList());
	}

	/**
	 * @param latestLogByDayId the member's most recent {@link WorkoutLog} per plan-day id
	 */
	static TrainingPlanResponse toResponse(TrainingPlan plan, Map<UUID, WorkoutLog> latestLogByDayId) {
		return new TrainingPlanResponse(
				plan.getId(),
				plan.getName(),
				plan.getNote(),
				plan.isTemplate(),
				plan.isActive(),
				plan.getUpdatedAt(),
				plan.getDays().stream().map(day -> toDayResponse(day, latestLogByDayId.get(day.getId()))).toList());
	}

	static PlanDayResponse toDayResponse(TrainingPlanDay day, WorkoutLog latestLog) {
		return toDayResponse(day, latestLog, latestLog == null ? List.of() : List.of(latestLog));
	}

	private static PlanDayResponse toDayResponse(TrainingPlanDay day, WorkoutLog latestLog,
			List<WorkoutLog> logsNewestFirst) {
		Map<UUID, List<SetLogResponse>> history = latestLog == null
				? Map.of()
				: latestLog.getSets().stream()
						.collect(Collectors.groupingBy(
								set -> set.getPlanExercise().getId(),
								Collectors.mapping(TrainingPlanMapper::toSetResponse, Collectors.toList())));
		LocalDate lastPerformedOn = latestLog == null ? null : latestLog.getPerformedOn();

		return new PlanDayResponse(
				day.getId(),
				day.getDayOrder(),
				day.getName(),
				day.getNote(),
				lastPerformedOn,
				day.getExercises().stream()
						.map(exercise -> toExerciseResponse(exercise,
								history.getOrDefault(exercise.getId(), List.of()),
								weightHistory(exercise, logsNewestFirst)))
						.toList());
	}

	static PlanExerciseResponse toExerciseResponse(TrainingPlanExercise planExercise,
			List<SetLogResponse> lastPerformed, List<WeightEntry> weightHistory) {
		return new PlanExerciseResponse(
				planExercise.getId(),
				ExerciseResponse.from(planExercise.getExercise()),
				planExercise.getDisplayOrder(),
				planExercise.getSetCount(),
				planExercise.getReps(),
				setRepsOf(planExercise),
				planExercise.getTargetWeightKg(),
				planExercise.getRestSeconds(),
				planExercise.getNote(),
				lastPerformed,
				weightHistory);
	}

	static List<String> setRepsOf(TrainingPlanExercise planExercise) {
		String stored = planExercise.getSetReps();
		return stored == null || stored.isBlank() ? List.of() : Arrays.asList(stored.split("\\|", -1));
	}

	/**
	 * The heaviest weight of each session in which this exercise was done, oldest first, kept to
	 * the last {@value #HISTORY_SESSIONS}. A session only counts if it recorded at least one set of
	 * this exercise; its weight is null when none of those sets had one.
	 */
	static List<WeightEntry> weightHistory(TrainingPlanExercise planExercise, List<WorkoutLog> logsNewestFirst) {
		List<WorkoutLog> oldestFirst = new ArrayList<>(logsNewestFirst);
		oldestFirst.sort(Comparator.comparing(WorkoutLog::getPerformedOn));

		List<WeightEntry> entries = new ArrayList<>();
		for (WorkoutLog log : oldestFirst) {
			List<SetLog> sets = log.getSets().stream()
					.filter(set -> set.getPlanExercise().getId().equals(planExercise.getId()))
					.toList();
			if (sets.isEmpty()) {
				continue;
			}
			BigDecimal top = sets.stream().map(SetLog::getWeightKg).filter(java.util.Objects::nonNull)
					.max(Comparator.naturalOrder()).orElse(null);
			entries.add(new WeightEntry(entries.size() + 1, log.getPerformedOn(), top));
		}
		return entries.size() <= HISTORY_SESSIONS
				? entries
				: List.copyOf(entries.subList(entries.size() - HISTORY_SESSIONS, entries.size()));
	}

	static SetLogResponse toSetResponse(SetLog setLog) {
		return new SetLogResponse(setLog.getPlanExercise().getId(), setLog.getSetNumber(), setLog.getReps(),
				setLog.getWeightKg(), setLog.getRpe());
	}
}
