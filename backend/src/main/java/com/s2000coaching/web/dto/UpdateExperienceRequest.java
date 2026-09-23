package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateExperienceRequest(@NotNull @Size(max = 20000) String content) {
}
