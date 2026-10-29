package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveMessageTemplateRequest(
		@NotBlank @Size(max = 80) String title,
		@NotBlank @Size(max = 4000) String body) {
}
