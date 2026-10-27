package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachReplyRequest(@NotBlank @Size(max = 3000) String reply) {
}
