package com.s2000coaching.web.dto;

import com.s2000coaching.domain.SuccessStoryPhoto;
import java.time.LocalDate;
import java.util.UUID;

public record StoryPhotoResponse(UUID id, String caption, LocalDate takenOn) {

	public static StoryPhotoResponse from(SuccessStoryPhoto photo) {
		return new StoryPhotoResponse(photo.getId(), photo.getCaption(), photo.getTakenOn());
	}
}
