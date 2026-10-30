package com.s2000coaching.web.dto;

import com.s2000coaching.domain.NutritionPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NutritionPlanResponse(
		UUID id,
		String name,
		String note,
		boolean template,
		boolean active,
		Integer targetCalories,
		Integer targetProteinG,
		Integer targetCarbsG,
		Integer targetFatG,
		BigDecimal waterLiters,
		Instant updatedAt,
		List<MealResponse> meals,
		/** The same four targets for rest days; the ones above are for training days. */
		Integer restTargetCalories,
		Integer restTargetProteinG,
		Integer restTargetCarbsG,
		Integer restTargetFatG) {

	/** The shape from before rest days existed: no rest-day targets. */
	public NutritionPlanResponse(UUID id, String name, String note, boolean template, boolean active,
			Integer targetCalories, Integer targetProteinG, Integer targetCarbsG, Integer targetFatG,
			BigDecimal waterLiters, Instant updatedAt, List<MealResponse> meals) {
		this(id, name, note, template, active, targetCalories, targetProteinG, targetCarbsG, targetFatG, waterLiters,
				updatedAt, meals, null, null, null, null);
	}

	public static NutritionPlanResponse from(NutritionPlan plan) {
		return new NutritionPlanResponse(
				plan.getId(),
				plan.getName(),
				plan.getNote(),
				plan.isTemplate(),
				plan.isActive(),
				plan.getTargetCalories(),
				plan.getTargetProteinG(),
				plan.getTargetCarbsG(),
				plan.getTargetFatG(),
				plan.getWaterLiters(),
				plan.getUpdatedAt(),
				plan.getMeals().stream().map(MealResponse::from).toList(),
				plan.getRestTargetCalories(),
				plan.getRestTargetProteinG(),
				plan.getRestTargetCarbsG(),
				plan.getRestTargetFatG());
	}
}
