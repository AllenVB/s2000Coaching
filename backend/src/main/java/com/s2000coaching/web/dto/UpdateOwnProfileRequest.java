package com.s2000coaching.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What a member may change about themselves — deliberately narrower than
 * {@link UpdateMemberRequest}, which is the coach's form.
 *
 * <p>E-mail is absent for the same reason it is absent there: it identifies the account and
 * receives the login codes, so changing it needs its own re-verification flow.
 *
 * <p>Starting weight is absent too. It is the baseline every progress chart is drawn against,
 * so letting a member rewrite it would silently redraw their own history. Members record
 * today's weight as a measurement instead.
 */
public record UpdateOwnProfileRequest(
		@NotBlank @Size(max = 160) String fullName,
		@NotBlank @Size(max = 40) String phone,
		@NotNull @Min(100) @Max(250) Integer heightCm,
		@NotBlank @Size(max = 5000) String sportsBackground) {
}
