package com.s2000coaching.web.dto;

import com.s2000coaching.domain.ExperienceCategory;
import java.util.List;
import java.util.UUID;

public record ExperienceCategoryResponse(UUID id, String name, List<ExperienceItemResponse> items) {

	public static ExperienceCategoryResponse from(ExperienceCategory category) {
		List<ExperienceItemResponse> items = category.getItems().stream()
				.map(ExperienceItemResponse::from)
				.toList();
		return new ExperienceCategoryResponse(category.getId(), category.getName(), items);
	}
}
