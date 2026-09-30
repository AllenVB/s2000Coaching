package com.s2000coaching.web.dto;

import com.s2000coaching.domain.ExperienceTopicNote;
import java.util.UUID;

public record ExperienceNoteResponse(UUID id, String content) {

	public static ExperienceNoteResponse from(ExperienceTopicNote note) {
		return new ExperienceNoteResponse(note.getId(), note.getContent());
	}
}
