package com.s2000coaching.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * One month's table. {@code period} ("YYYY-MM") names the month when it is created and is ignored
 * when an existing month is edited. Every figure is optional; the ranges only keep out typos
 * (a weight of 8, a waist of 800).
 */
public record UpsertMonthlyRecordRequest(
		@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Ay AAAA-AA biçiminde olmalı") String period,
		@DecimalMin("100.0") @DecimalMax("250.0") BigDecimal heightCm,
		@DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
		@DecimalMin("20.0") @DecimalMax("250.0") BigDecimal shoulderCm,
		@DecimalMin("20.0") @DecimalMax("250.0") BigDecimal chestCm,
		@DecimalMin("10.0") @DecimalMax("100.0") BigDecimal armCm,
		@DecimalMin("15.0") @DecimalMax("80.0") BigDecimal neckCm,
		@DecimalMin("20.0") @DecimalMax("250.0") BigDecimal waistCm,
		@DecimalMin("30.0") @DecimalMax("250.0") BigDecimal hipCm,
		@DecimalMin("15.0") @DecimalMax("120.0") BigDecimal thighCm) {

	/** True when the table has at least one figure; an entirely empty month is not worth saving. */
	public boolean hasAnyFigure() {
		return heightCm != null || weightKg != null || shoulderCm != null || chestCm != null || armCm != null
				|| neckCm != null || waistCm != null || hipCm != null || thighCm != null;
	}
}
