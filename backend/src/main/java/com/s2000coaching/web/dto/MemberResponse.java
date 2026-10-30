package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberGender;
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
		boolean remindersEnabled,
		MemberGender gender,
		Integer age,
		/** Whether a password is set; false means the member still signs in with an e-mailed code. */
		boolean hasPassword,
		/** Five digits, assigned once; shown to people as "#12345". */
		int memberNumber) {

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
				member.isRemindersEnabled(),
				member.getGender(),
				member.getAge(),
				member.hasPassword(),
				member.getMemberNumber());
	}
}
