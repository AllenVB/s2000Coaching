package com.s2000coaching.web.dto;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.domain.ExperienceNote;
import java.time.Instant;

public record ExperienceNoteResponse(ExperienceCategory category, String content, Instant updatedAt) {

	public static ExperienceNoteResponse from(ExperienceNote note) {
		return new ExperienceNoteResponse(note.getCategory(), note.getContent(), note.getUpdatedAt());
	}
}
