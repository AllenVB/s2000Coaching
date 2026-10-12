package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Macro targets are optional: a coach may prescribe meals without counting. */
public record UpsertNutritionPlanRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 2000) String note,
		@Min(500) @Max(10000) Integer targetCalories,
		@Min(0) @Max(1000) Integer targetProteinG,
		@Min(0) @Max(2000) Integer targetCarbsG,
		@Min(0) @Max(500) Integer targetFatG) {
}
