package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.MuscleGroup;
import com.s2000coaching.service.YouTubeLink;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code video} is the id the page builds its player from, parsed here rather than in the
 * browser so there is one definition of what counts as a YouTube link. It is null for an
 * exercise with no video, and also for an old catalogue entry that holds some other address —
 * {@code videoUrl} still carries that, so the page can offer it as a plain link.
 */
public record ExerciseResponse(UUID id, String name, MuscleGroup muscleGroup, String videoUrl, Video video,
		String note, boolean hasImage, Instant imageUpdatedAt) {

	public record Video(String id, int startSeconds) {
	}

	public static ExerciseResponse from(Exercise exercise) {
		Video video = YouTubeLink.parse(exercise.getVideoUrl())
				.map(parsed -> new Video(parsed.videoId(), parsed.startSeconds()))
				.orElse(null);
		return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
				exercise.getVideoUrl(), video, exercise.getNote(), exercise.hasImage(), exercise.getImageUpdatedAt());
	}
}
