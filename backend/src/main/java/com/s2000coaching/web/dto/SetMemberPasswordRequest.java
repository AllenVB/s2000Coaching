package com.s2000coaching.web.dto;

import com.s2000coaching.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The coach choosing a new password for a member. The member's own sessions end when it is set. */
public record SetMemberPasswordRequest(@NotBlank @StrongPassword String newPassword) {

	@Override
	public String toString() {
		return "SetMemberPasswordRequest[***]";
	}
}
