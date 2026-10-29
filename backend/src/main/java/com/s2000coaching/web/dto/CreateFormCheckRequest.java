package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFormCheckRequest(
		@NotBlank @Size(max = 120) String exerciseName,
		@NotBlank @Size(max = 500) String videoUrl,
		@Size(max = 2000) String note) {
}
