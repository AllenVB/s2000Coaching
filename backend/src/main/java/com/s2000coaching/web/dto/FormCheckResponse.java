package com.s2000coaching.web.dto;

import com.s2000coaching.domain.FormCheck;
import com.s2000coaching.service.YouTubeLink;
import java.time.Instant;
import java.util.UUID;

/** {@code video} is what the page builds its player from; the stored address is always YouTube. */
public record FormCheckResponse(UUID id, String exerciseName, ExerciseResponse.Video video, String note,
		String coachReply, Instant coachRepliedAt, Instant createdAt) {

	public static FormCheckResponse from(FormCheck check) {
		var video = YouTubeLink.parse(check.getVideoUrl())
				.map(parsed -> new ExerciseResponse.Video(parsed.videoId(), parsed.startSeconds()))
				.orElse(null);
		return new FormCheckResponse(check.getId(), check.getExerciseName(), video, check.getNote(),
				check.getCoachReply(), check.getCoachRepliedAt(), check.getCreatedAt());
	}
}
