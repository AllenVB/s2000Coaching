package com.s2000coaching.web.dto;

import java.time.Instant;

/** Returned once after a successful code verification; {@code token} is never retrievable again. */
public record MemberSessionResponse(String token, Instant expiresAt, MemberResponse member) {
}
