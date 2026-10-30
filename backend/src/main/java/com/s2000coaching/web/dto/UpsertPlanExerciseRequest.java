package com.s2000coaching.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The exercise is named either by {@code exerciseId} (one already in the catalogue) or by
 * {@code exerciseName} (typed by the coach; found or created in the catalogue). Either
 * {@code setReps} (the rep target of each set, up to ten — none beyond the first required)
 * or the older {@code setCount} with a free-text {@code reps} ("8-10", "AMRAP"). When
 * {@code setReps} is given it wins, and the other two are derived from it.
 */
public record UpsertPlanExerciseRequest(
		UUID exerciseId,
		@Size(max = 120) String exerciseName,
		@Min(1) @Max(20) Integer setCount,
		@Size(max = 40) String reps,
		@Size(max = 10) List<@Size(max = 16) String> setReps,
		@DecimalMin("0.0") @DecimalMax("999.0") BigDecimal targetWeightKg,
		@Min(0) @Max(900) Integer restSeconds,
		@Size(max = 2000) String note) {

	/** Most sets one exercise can have. */
	public static final int MAX_SETS = 10;

	/** The legacy shape: a set count and one free-text reps for all of them. */
	public UpsertPlanExerciseRequest(UUID exerciseId, Integer setCount, String reps, BigDecimal targetWeightKg,
			Integer restSeconds, String note) {
		this(exerciseId, null, setCount, reps, null, targetWeightKg, restSeconds, note);
	}
}
