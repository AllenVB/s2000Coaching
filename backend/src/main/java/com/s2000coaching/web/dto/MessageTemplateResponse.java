package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MessageTemplate;
import java.time.Instant;
import java.util.UUID;

public record MessageTemplateResponse(UUID id, String title, String body, Instant updatedAt) {

	public static MessageTemplateResponse from(MessageTemplate template) {
		return new MessageTemplateResponse(template.getId(), template.getTitle(), template.getBody(),
				template.getUpdatedAt());
	}
}
