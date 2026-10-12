package com.s2000coaching.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** {@code reps} is free text ("8-10", "AMRAP") because that is how programmes are written. */
public record UpsertPlanExerciseRequest(
		@NotNull UUID exerciseId,
		@NotNull @Min(1) @Max(20) Integer setCount,
		@NotBlank @Size(max = 40) String reps,
		@DecimalMin("0.0") @DecimalMax("999.0") BigDecimal targetWeightKg,
		@Min(0) @Max(900) Integer restSeconds,
		@Size(max = 2000) String note) {
}
