package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MemberGender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateMemberRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 160) String fullName,
		@Size(max = 40) String phone,
		MemberGender gender,
		@Min(14) @Max(100) Integer age,
		@NotNull @Min(100) @Max(250) Integer heightCm,
		@NotNull @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
		@NotBlank @Size(max = 5000) String sportsBackground) {
}
