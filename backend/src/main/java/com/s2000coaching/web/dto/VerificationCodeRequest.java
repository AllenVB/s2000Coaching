package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin-side completion of a registration: the member relays the code they received. */
public record VerificationCodeRequest(@NotBlank @Size(max = 12) String code) {
}
