package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MemberGender;
import com.s2000coaching.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Self-service sign-up. The password is capped at 64 characters because bcrypt only reads the
 * first 72 bytes: a longer one would be accepted and silently cut short.
 */
public record RegisterMemberRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @StrongPassword String password,
		@NotBlank @Size(max = 80) String firstName,
		@NotBlank @Size(max = 80) String lastName,
		@NotNull @Min(14) @Max(100) Integer age,
		@NotNull MemberGender gender,
		@NotNull @Min(100) @Max(250) Integer heightCm,
		@NotNull @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
		@NotBlank @Size(max = 5000) String sportsBackground,
		@AssertTrue(message = "Aydınlatma metnini kabul etmeden hesap oluşturulamaz.") boolean acceptedTerms) {

	/** A password must never reach a log line, whoever logs the request. */
	@Override
	public String toString() {
		return "RegisterMemberRequest[email=" + email + ", password=***]";
	}
}
