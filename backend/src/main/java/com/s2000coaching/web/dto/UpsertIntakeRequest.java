package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One day's totals. Calories are required; macros, sodium and water are optional. */
public record UpsertIntakeRequest(
		@NotNull @PastOrPresent LocalDate consumedOn,
		@NotNull @Min(0) @Max(20000) Integer calories,
		@Min(0) @Max(2000) Integer proteinG,
		@Min(0) @Max(3000) Integer carbsG,
		@Min(0) @Max(2000) Integer fatG,
		@Min(0) @Max(50000) Integer sodiumMg,
		@Size(max = 1000) String note,
		@jakarta.validation.constraints.DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("20.0")
		java.math.BigDecimal waterLiters) {

	/** The shape from before water was recorded. */
	public UpsertIntakeRequest(LocalDate consumedOn, Integer calories, Integer proteinG, Integer carbsG, Integer fatG,
			Integer sodiumMg, String note) {
		this(consumedOn, calories, proteinG, carbsG, fatG, sodiumMg, note, null);
	}
}
