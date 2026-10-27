package com.s2000coaching.web.dto;

import com.s2000coaching.domain.SuccessStory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What the public site gets: no member id, no real name — the display name the member agreed
 * to — and nothing about consent or status, because only published stories are ever returned.
 */
public record PublicStoryResponse(UUID id, String title, String quote, String body, String displayName,
		Instant publishedAt, List<StoryPhotoResponse> photos) {

	public static PublicStoryResponse from(SuccessStory story, List<StoryPhotoResponse> photos) {
		return new PublicStoryResponse(story.getId(), story.getTitle(), story.getQuote(), story.getBody(),
				story.getDisplayName(), story.getPublishedAt(), photos);
	}
}
