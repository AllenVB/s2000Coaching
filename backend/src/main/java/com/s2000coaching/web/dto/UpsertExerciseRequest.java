package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertExerciseRequest(
		@NotBlank @Size(max = 120) String name,
		MuscleGroup muscleGroup,
		@Size(max = 500) String videoUrl,
		@Size(max = 2000) String note) {
}
