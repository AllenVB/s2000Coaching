package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The coach choosing a new password for a member. The member's own sessions end when it is set. */
public record SetMemberPasswordRequest(@NotBlank @Size(min = 8, max = 64) String newPassword) {

	@Override
	public String toString() {
		return "SetMemberPasswordRequest[***]";
	}
}
