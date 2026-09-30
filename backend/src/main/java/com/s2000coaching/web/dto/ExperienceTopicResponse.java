package com.s2000coaching.web.dto;

import com.s2000coaching.domain.ExperienceTopic;
import java.util.List;
import java.util.UUID;

public record ExperienceTopicResponse(UUID id, String name, List<ExperienceNoteResponse> notes) {

	public static ExperienceTopicResponse from(ExperienceTopic topic) {
		List<ExperienceNoteResponse> notes = topic.getNotes().stream()
				.map(ExperienceNoteResponse::from)
				.toList();
		return new ExperienceTopicResponse(topic.getId(), topic.getName(), notes);
	}
}
