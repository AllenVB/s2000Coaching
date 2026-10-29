package com.s2000coaching.web.dto;

import com.s2000coaching.domain.StoryStatus;
import com.s2000coaching.domain.SuccessStory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A story as the coach and the member see it. {@code memberName} is only set for the coach;
 * {@code consentText} only while the member is being asked, so they read the wording they are
 * agreeing to next to the content it applies to.
 */
public record StoryResponse(UUID id, UUID memberId, String memberName, String title, String quote, String body,
		String displayName, StoryStatus status, Instant consentAt, Instant publishedAt, Instant updatedAt,
		List<StoryPhotoResponse> photos, String consentText) {

	public static StoryResponse forCoach(SuccessStory story, List<StoryPhotoResponse> photos) {
		return new StoryResponse(story.getId(), story.getMember().getId(), story.getMember().getFullName(),
				story.getTitle(), story.getQuote(), story.getBody(), story.getDisplayName(), story.getStatus(),
				story.getConsentAt(), story.getPublishedAt(), story.getUpdatedAt(), photos, null);
	}

	public static StoryResponse forMember(SuccessStory story, List<StoryPhotoResponse> photos, String consentText) {
		return new StoryResponse(story.getId(), story.getMember().getId(), null, story.getTitle(), story.getQuote(),
				story.getBody(), story.getDisplayName(), story.getStatus(), story.getConsentAt(),
				story.getPublishedAt(), story.getUpdatedAt(), photos,
				story.getStatus() == StoryStatus.AWAITING_CONSENT ? consentText : null);
	}
}
