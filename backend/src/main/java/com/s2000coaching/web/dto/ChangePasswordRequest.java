package com.s2000coaching.web.dto;

import com.s2000coaching.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code currentPassword} is required when a password is already set, and ignored when it is not. */
public record ChangePasswordRequest(
		@Size(max = 200) String currentPassword,
		@NotBlank @StrongPassword String newPassword) {

	@Override
	public String toString() {
		return "ChangePasswordRequest[***]";
	}
}
