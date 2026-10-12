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
 * Editable member fields. E-mail is deliberately absent: it identifies the account and is
 * what verification codes are sent to, so changing it needs its own re-verification flow.
 */
public record UpdateMemberRequest(
		@NotBlank @Size(max = 160) String fullName,
		@NotBlank @Size(max = 40) String phone,
		@NotNull @Min(100) @Max(250) Integer heightCm,
		@NotNull @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
		@NotBlank @Size(max = 5000) String sportsBackground) {
}
