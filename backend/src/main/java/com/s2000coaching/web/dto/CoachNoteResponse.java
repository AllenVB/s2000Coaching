package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CoachNote;
import java.time.Instant;
import java.util.UUID;

public record CoachNoteResponse(UUID id, String body, Instant createdAt, Instant updatedAt) {

	public static CoachNoteResponse from(CoachNote note) {
		return new CoachNoteResponse(note.getId(), note.getBody(), note.getCreatedAt(), note.getUpdatedAt());
	}
}
