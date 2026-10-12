package com.s2000coaching.web.dto;

import com.s2000coaching.domain.ExperienceCategory;
import java.util.List;
import java.util.UUID;

public record ExperienceCategoryResponse(UUID id, String name, List<ExperienceTopicResponse> topics) {

	public static ExperienceCategoryResponse from(ExperienceCategory category) {
		List<ExperienceTopicResponse> topics = category.getTopics().stream()
				.map(ExperienceTopicResponse::from)
				.toList();
		return new ExperienceCategoryResponse(category.getId(), category.getName(), topics);
	}
}
