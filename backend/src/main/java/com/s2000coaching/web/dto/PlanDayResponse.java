package com.s2000coaching.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** @param lastPerformedOn date of the member's most recent session of this day, null if none */
public record PlanDayResponse(
		UUID id,
		int dayOrder,
		String name,
		String note,
		LocalDate lastPerformedOn,
		List<PlanExerciseResponse> exercises) {
}
