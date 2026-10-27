package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** {@code weekOf} is any day of the week being reported; the server uses that week's Monday. */
public record UpsertCheckInRequest(
		@NotNull @PastOrPresent LocalDate weekOf,
		@NotNull @Min(1) @Max(5) Integer sleep,
		@NotNull @Min(1) @Max(5) Integer stress,
		@NotNull @Min(1) @Max(5) Integer energy,
		@NotNull @Min(1) @Max(5) Integer hunger,
		@Min(0) @Max(100) Integer dietAdherencePercent,
		@Size(max = 3000) String note) {
}
