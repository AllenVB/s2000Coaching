package com.s2000coaching.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Macro targets are optional: a coach may prescribe meals without counting. */
public record UpsertNutritionPlanRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 2000) String note,
		@Min(500) @Max(10000) Integer targetCalories,
		@Min(0) @Max(1000) Integer targetProteinG,
		@Min(0) @Max(2000) Integer targetCarbsG,
		@Min(0) @Max(500) Integer targetFatG,
		@DecimalMin("0.0") @DecimalMax("20.0") BigDecimal waterLiters,
		/** The same four targets for rest days; the ones above are for training days. */
		@Min(500) @Max(10000) Integer restTargetCalories,
		@Min(0) @Max(1000) Integer restTargetProteinG,
		@Min(0) @Max(2000) Integer restTargetCarbsG,
		@Min(0) @Max(500) Integer restTargetFatG) {

	/** Training-day targets and water only, as before rest days existed. */
	public UpsertNutritionPlanRequest(String name, String note, Integer targetCalories, Integer targetProteinG,
			Integer targetCarbsG, Integer targetFatG, BigDecimal waterLiters) {
		this(name, note, targetCalories, targetProteinG, targetCarbsG, targetFatG, waterLiters, null, null, null,
				null);
	}
}
