package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Meal;
import java.util.List;
import java.util.UUID;

public record MealResponse(
		UUID id,
		int displayOrder,
		String name,
		String timeHint,
		String note,
		List<MealItemResponse> items) {

	public static MealResponse from(Meal meal) {
		return new MealResponse(meal.getId(), meal.getDisplayOrder(), meal.getName(), meal.getTimeHint(),
				meal.getNote(), meal.getItems().stream().map(MealItemResponse::from).toList());
	}
}
