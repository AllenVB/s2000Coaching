package com.s2000coaching.web.dto;

import com.s2000coaching.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The minimum length is on the new password only: the current one is whatever the account
 * already has, and rejecting it here for being short would lock out the very accounts that
 * most need a change.
 */
public record ChangeCoachPasswordRequest(
		@NotBlank @Size(max = 200) String currentPassword,
		@NotBlank @StrongPassword(min = 10, max = 200) String newPassword) {

	/** A password must never reach a log line, whoever logs the request. */
	@Override
	public String toString() {
		return "ChangeCoachPasswordRequest[passwords=***]";
	}
}
