package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/** One day's steps and cardio minutes; at least one is checked by the service. */
public record UpsertActivityRequest(
		@NotNull @PastOrPresent LocalDate performedOn,
		@Min(0) @Max(200000) Integer steps,
		@Min(0) @Max(1440) Integer cardioMinutes) {
}
