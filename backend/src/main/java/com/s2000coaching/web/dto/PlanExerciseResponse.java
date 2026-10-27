package com.s2000coaching.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A prescribed exercise. {@code lastPerformed} carries what the member recorded in their most
 * recent session of this day, so the member area can show "geçen sefer" beside the target; it
 * is empty on the coach side. {@code setReps} is the rep target per set as the coach wrote them
 * (empty for an exercise written with one free-text reps), and {@code weightHistory} is the top
 * weight of each of the member's sessions of this exercise, oldest first — the "Antrenman 1–10"
 * strip is drawn from it.
 */
public record PlanExerciseResponse(
		UUID id,
		ExerciseResponse exercise,
		int displayOrder,
		int setCount,
		String reps,
		List<String> setReps,
		BigDecimal targetWeightKg,
		Integer restSeconds,
		String note,
		List<SetLogResponse> lastPerformed,
		List<WeightEntry> weightHistory) {
}
