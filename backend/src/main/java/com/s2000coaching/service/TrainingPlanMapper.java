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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds plan responses. The coach gets the plan as written; the member additionally gets what
 * they did last time, which is the number that actually drives the next session.
 */
final class TrainingPlanMapper {

	private TrainingPlanMapper() {
	}

	/** The plan as written, with no training history attached. */
	static TrainingPlanResponse toResponse(TrainingPlan plan) {
		return toResponse(plan, Map.of());
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
								history.getOrDefault(exercise.getId(), List.of())))
						.toList());
	}

	static PlanExerciseResponse toExerciseResponse(TrainingPlanExercise planExercise,
			List<SetLogResponse> lastPerformed) {
		return new PlanExerciseResponse(
				planExercise.getId(),
				ExerciseResponse.from(planExercise.getExercise()),
				planExercise.getDisplayOrder(),
				planExercise.getSetCount(),
				planExercise.getReps(),
				planExercise.getTargetWeightKg(),
				planExercise.getRestSeconds(),
				planExercise.getNote(),
				lastPerformed);
	}

	static SetLogResponse toSetResponse(SetLog setLog) {
		return new SetLogResponse(setLog.getPlanExercise().getId(), setLog.getSetNumber(), setLog.getReps(),
				setLog.getWeightKg());
	}
}
