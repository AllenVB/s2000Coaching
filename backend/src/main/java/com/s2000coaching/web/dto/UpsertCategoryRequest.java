package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertCategoryRequest(@NotBlank @Size(max = 60) String name) {
}
