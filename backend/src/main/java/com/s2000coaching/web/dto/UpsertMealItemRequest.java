package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertMealItemRequest(
		@NotBlank @Size(max = 160) String name,
		@Size(max = 60) String amount,
		@Min(0) @Max(5000) Integer calories,
		@Min(0) @Max(500) Integer proteinG,
		@Min(0) @Max(1000) Integer carbsG,
		@Min(0) @Max(500) Integer fatG,
		@Size(max = 2000) String note) {
}
