package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MealKind;
import com.s2000coaching.domain.NutritionDayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertMealRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 60) String timeHint,
		@Size(max = 2000) String note,
		/** Null means an ordinary meal. */
		MealKind kind,
		/** Which half of the plan a new meal goes into; null means training days. Ignored when editing. */
		NutritionDayType dayType) {

	public UpsertMealRequest(String name, String timeHint, String note, MealKind kind) {
		this(name, timeHint, note, kind, null);
	}
}
