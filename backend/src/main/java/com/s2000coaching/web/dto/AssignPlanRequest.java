package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** The members who each get their own copy of a plan. */
public record AssignPlanRequest(@NotEmpty @Size(max = 200) List<UUID> memberIds) {
}
