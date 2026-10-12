package com.s2000coaching.web.dto;

import java.time.Instant;

/** Returned once on login; {@code token} is never retrievable again. */
public record CoachSessionResponse(String token, Instant expiresAt, CoachResponse coach) {
}
