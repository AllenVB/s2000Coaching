package com.s2000coaching.web.dto;

import com.s2000coaching.domain.WorkoutLog;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkoutLogResponse(
		UUID id,
		UUID dayId,
		String dayName,
		LocalDate performedOn,
		String note,
		List<SetLogResponse> sets) {

	public static WorkoutLogResponse from(WorkoutLog log) {
		return new WorkoutLogResponse(
				log.getId(),
				log.getDay().getId(),
				log.getDay().getName(),
				log.getPerformedOn(),
				log.getNote(),
				log.getSets().stream()
						.map(set -> new SetLogResponse(set.getPlanExercise().getId(), set.getSetNumber(),
								set.getReps(), set.getWeightKg()))
						.toList());
	}
}
