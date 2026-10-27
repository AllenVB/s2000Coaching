package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A membership the coach adds by hand, for money that arrived outside the shop. */
public record GrantMembershipRequest(
		@NotBlank @Size(max = 200) String label,
		@NotNull @Min(1) @Max(36) Integer months) {
}
