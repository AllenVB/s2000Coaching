package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
		UUID id,
		String email,
		String fullName,
		String phone,
		int heightCm,
		BigDecimal weightKg,
		String sportsBackground,
		MemberStatus status,
		Instant createdAt,
		Instant verifiedAt,
		Instant healthDataConsentAt,
		boolean remindersEnabled) {

	public static MemberResponse from(Member member) {
		return new MemberResponse(
				member.getId(),
				member.getEmail(),
				member.getFullName(),
				member.getPhone(),
				member.getHeightCm(),
				member.getWeightKg(),
				member.getSportsBackground(),
				member.getStatus(),
				member.getCreatedAt(),
				member.getVerifiedAt(),
				member.getHealthDataConsentAt(),
				member.isRemindersEnabled());
	}
}
