package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Asks for a fresh one-time code to be mailed to this address. */
public record MemberEmailRequest(@NotBlank @Email @Size(max = 255) String email) {
}
