package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Member-side code submission: completes a pending registration and/or signs the member in. */
public record MemberCodeVerificationRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 12) String code) {
}
