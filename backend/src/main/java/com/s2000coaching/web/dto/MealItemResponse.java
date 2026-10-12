package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MealItem;
import java.util.UUID;

public record MealItemResponse(
		UUID id,
		int displayOrder,
		String name,
		String amount,
		Integer calories,
		Integer proteinG,
		Integer carbsG,
		Integer fatG,
		String note) {

	public static MealItemResponse from(MealItem item) {
		return new MealItemResponse(item.getId(), item.getDisplayOrder(), item.getName(), item.getAmount(),
				item.getCalories(), item.getProteinG(), item.getCarbsG(), item.getFatG(), item.getNote());
	}
}
