package com.s2000coaching.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A whole session in one request; saving again for the same day replaces it. */
public record SaveWorkoutLogRequest(
		@NotNull UUID dayId,
		@NotNull @PastOrPresent LocalDate performedOn,
		@Size(max = 2000) String note,
		@NotNull @Valid List<SetEntry> sets) {

	public record SetEntry(
			@NotNull UUID planExerciseId,
			@NotNull @Min(1) @Max(50) Integer setNumber,
			@NotNull @Min(0) @Max(1000) Integer reps,
			@DecimalMin("0.0") @DecimalMax("999.0") BigDecimal weightKg,
			@DecimalMin("1.0") @DecimalMax("10.0") BigDecimal rpe) {
	}
}
