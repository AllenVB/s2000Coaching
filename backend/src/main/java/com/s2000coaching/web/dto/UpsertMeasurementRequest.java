package com.s2000coaching.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A check-in. Weight is required; the circumferences are optional so a member who only steps
 * on the scale can still log the week.
 */
public record UpsertMeasurementRequest(
		@NotNull @PastOrPresent LocalDate measuredOn,
		@NotNull @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
		@DecimalMin("30.0") @DecimalMax("250.0") BigDecimal waistCm,
		@DecimalMin("30.0") @DecimalMax("250.0") BigDecimal chestCm,
		@DecimalMin("30.0") @DecimalMax("250.0") BigDecimal hipCm,
		@DecimalMin("15.0") @DecimalMax("100.0") BigDecimal armCm,
		@DecimalMin("20.0") @DecimalMax("150.0") BigDecimal thighCm,
		@Size(max = 2000) String note) {
}
