package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertMealRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 60) String timeHint,
		@Size(max = 2000) String note) {
}
