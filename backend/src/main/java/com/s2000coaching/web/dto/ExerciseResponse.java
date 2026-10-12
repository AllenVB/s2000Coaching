package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.MuscleGroup;
import java.util.UUID;

public record ExerciseResponse(UUID id, String name, MuscleGroup muscleGroup, String videoUrl, String note) {

	public static ExerciseResponse from(Exercise exercise) {
		return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
				exercise.getVideoUrl(), exercise.getNote());
	}
}
