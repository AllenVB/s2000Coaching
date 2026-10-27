package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStoryRequest(
		@NotBlank @Size(max = 160) String title,
		@Size(max = 400) String quote,
		@NotBlank @Size(max = 5000) String body,
		@NotBlank @Size(max = 80) String displayName) {
}
