package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertItemRequest(
		@NotBlank @Size(max = 160) String title,
		@NotBlank @Size(max = 5000) String description) {
}
