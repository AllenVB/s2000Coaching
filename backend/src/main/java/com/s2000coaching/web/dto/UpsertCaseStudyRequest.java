package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Sex;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Height, sex and birth year are optional because they exist only to feed the body-fat
 * formulas. Without them the analysis simply omits that estimate rather than guessing.
 */
public record UpsertCaseStudyRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 2000) String summary,
		@Min(100) @Max(250) Integer heightCm,
		Sex sex,
		@Min(1900) @Max(2100) Integer birthYear) {
}
