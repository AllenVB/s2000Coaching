package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The e-mailed code proves the address; with it the member chooses a new password. */
public record ResetPasswordRequest(
		@NotBlank @Size(max = 255) String email,
		@NotBlank @Size(min = 6, max = 6) String code,
		@NotBlank @Size(min = 8, max = 64) String newPassword) {

	@Override
	public String toString() {
		return "ResetPasswordRequest[email=" + email + ", code=***, newPassword=***]";
	}
}
