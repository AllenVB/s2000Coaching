package com.s2000coaching.web.dto;

import com.s2000coaching.domain.ExperienceItem;
import java.time.Instant;
import java.util.UUID;

public record ExperienceItemResponse(UUID id, String title, String description, Instant updatedAt) {

	public static ExperienceItemResponse from(ExperienceItem item) {
		return new ExperienceItemResponse(item.getId(), item.getTitle(), item.getDescription(), item.getUpdatedAt());
	}
}
