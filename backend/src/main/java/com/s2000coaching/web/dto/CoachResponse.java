package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CoachAccount;
import java.time.Instant;
import java.util.UUID;

/** Never carries the password hash. */
public record CoachResponse(UUID id, String email, String fullName, Instant lastLoginAt) {

	public static CoachResponse from(CoachAccount coach) {
		return new CoachResponse(coach.getId(), coach.getEmail(), coach.getFullName(),
				coach.getLastLoginAt());
	}
}
