package com.s2000coaching.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * What a member may change about themselves: age, phone, height, weight and sports background.
 * Name and gender are the coach's to change, and so is the e-mail address, which identifies the
 * account and receives its codes.
 *
 * <p>Weight here is the figure the member's profile and charts start from; today's weight is
 * still recorded as a measurement under Ölçümler.
 */
public record UpdateOwnProfileRequest(
		@NotNull @Min(14) @Max(100) Integer age,
		@Size(max = 40) String phone,
		@NotNull @Min(100) @Max(250) Integer heightCm,
		@NotNull @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
		@NotBlank @Size(max = 5000) String sportsBackground) {
}
