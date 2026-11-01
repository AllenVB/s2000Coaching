package com.s2000coaching.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** The public pre-assessment form. {@code website} is a honeypot: people leave it empty, form-filling bots do not. */
public record CreateLeadRequest(
		@NotBlank @Size(max = 160) String fullName,
		@NotBlank @Size(min = 7, max = 40) String phone,
		@Email @Size(max = 255) String email,
		@NotBlank @Size(max = 40) String goal,
		@Size(max = 40) String level,
		@Min(100) @Max(250) Integer heightCm,
		@jakarta.validation.constraints.DecimalMin("30.0") @jakarta.validation.constraints.DecimalMax("300.0")
		BigDecimal weightKg,
		@Size(max = 1000) String note,
		@AssertTrue(message = "İletişim onayı zorunludur") boolean consent,
		@Size(max = 200) String website) {
}
