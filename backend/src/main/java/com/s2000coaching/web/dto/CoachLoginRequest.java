package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachLoginRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 200) String password) {

	/** A password must never reach a log line, whoever logs the request. */
	@Override
	public String toString() {
		return "CoachLoginRequest[email=" + email + ", password=***]";
	}
}
