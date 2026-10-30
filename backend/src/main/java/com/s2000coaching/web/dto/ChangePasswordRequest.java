package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code currentPassword} is required when a password is already set, and ignored when it is not. */
public record ChangePasswordRequest(
		@Size(max = 200) String currentPassword,
		@NotBlank @Size(min = 8, max = 64) String newPassword) {

	@Override
	public String toString() {
		return "ChangePasswordRequest[***]";
	}
}
