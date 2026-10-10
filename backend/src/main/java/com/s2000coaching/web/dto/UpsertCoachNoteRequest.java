package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertCoachNoteRequest(@NotBlank @Size(max = 5000) String body) {
}
