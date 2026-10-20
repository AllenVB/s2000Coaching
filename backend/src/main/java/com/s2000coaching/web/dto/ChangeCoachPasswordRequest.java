package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The minimum length is on the new password only: the current one is whatever the account
 * already has, and rejecting it here for being short would lock out the very accounts that
 * most need a change.
 */
public record ChangeCoachPasswordRequest(
		@NotBlank @Size(max = 200) String currentPassword,
		@NotBlank @Size(min = 10, max = 200) String newPassword) {
}
