package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberLoginRequest(
		@NotBlank @Size(max = 255) String email,
		@NotBlank @Size(max = 200) String password) {

	@Override
	public String toString() {
		return "MemberLoginRequest[email=" + email + ", password=***]";
	}
}
