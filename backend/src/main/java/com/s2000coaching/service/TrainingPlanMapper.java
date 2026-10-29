package com.s2000coaching.service;

import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.repository.SessionTopWeight;
import com.s2000coaching.web.dto.ExerciseResponse;
import com.s2000coaching.web.dto.PlanDayResponse;
import com.s2000coaching.web.dto.PlanExerciseResponse;
import com.s2000coaching.web.dto.SetLogResponse;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.WeightEntry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	 * The member's view: each day carries their latest session, and each exercise the weight of
	 * every session they did of it.
	 *
	 * @param latestLogByDayId the member's most recent {@link WorkoutLog} per plan-day id
	 * @param historyByPlanExerciseId {@link #weightHistories} for the plan's exercises
	 */
	static TrainingPlanResponse toMemberResponse(TrainingPlan plan, Map<UUID, WorkoutLog> latestLogByDayId,
			Map<UUID, List<WeightEntry>> historyByPlanExerciseId) {
		return new TrainingPlanResponse(
				plan.getId(),
				plan.getName(),
				plan.getNote(),
				plan.isTemplate(),
				plan.isActive(),
				plan.getUpdatedAt(),
				plan.getDays().stream()
						.map(day -> toDayResponse(day, latestLogByDayId.get(day.getId()), historyByPlanExerciseId))
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
		return toDayResponse(day, latestLog, Map.of());
	}

	private static PlanDayResponse toDayResponse(TrainingPlanDay day, WorkoutLog latestLog,
			Map<UUID, List<WeightEntry>> historyByPlanExerciseId) {
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
								historyByPlanExerciseId.getOrDefault(exercise.getId(), List.of())))
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
	 * Numbers each exercise's sessions from 1 and keeps the last {@value #HISTORY_SESSIONS}, with
	 * their true numbers, so "Antrenman 3" stays "Antrenman 3" as the window slides on.
	 *
	 * @param oldestFirst one row per exercise and session, ordered by date
	 */
	static Map<UUID, List<WeightEntry>> weightHistories(List<SessionTopWeight> oldestFirst) {
		Map<UUID, List<SessionTopWeight>> byExercise = oldestFirst.stream()
				.collect(Collectors.groupingBy(SessionTopWeight::planExerciseId, LinkedHashMap::new,
						Collectors.toList()));

		Map<UUID, List<WeightEntry>> histories = new HashMap<>();
		byExercise.forEach((planExerciseId, sessions) -> {
			List<WeightEntry> entries = new ArrayList<>();
			for (SessionTopWeight session : sessions) {
				entries.add(new WeightEntry(entries.size() + 1, session.performedOn(), session.topWeightKg()));
			}
			histories.put(planExerciseId, entries.size() <= HISTORY_SESSIONS
					? entries
					: List.copyOf(entries.subList(entries.size() - HISTORY_SESSIONS, entries.size())));
		});
		return histories;
	}

	static SetLogResponse toSetResponse(SetLog setLog) {
		return new SetLogResponse(setLog.getPlanExercise().getId(), setLog.getSetNumber(), setLog.getReps(),
				setLog.getWeightKg(), setLog.getRpe());
	}
}
