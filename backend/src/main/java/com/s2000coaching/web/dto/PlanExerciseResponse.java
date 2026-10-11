package com.s2000coaching.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A prescribed exercise. {@code lastPerformed} carries what the member recorded in their most
 * recent session of this day, so the member area can show "geçen sefer" beside the target; it
 * is empty on the coach side.
 */
public record PlanExerciseResponse(
		UUID id,
		ExerciseResponse exercise,
		int displayOrder,
		int setCount,
		String reps,
		BigDecimal targetWeightKg,
		Integer restSeconds,
		String note,
		List<SetLogResponse> lastPerformed) {
}
