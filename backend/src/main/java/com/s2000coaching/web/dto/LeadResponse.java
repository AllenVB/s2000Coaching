package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Lead;
import com.s2000coaching.domain.LeadStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(UUID id, String fullName, String phone, String email, String goal, String level,
		Integer heightCm, BigDecimal weightKg, String note, LeadStatus status, Instant createdAt) {

	public static LeadResponse from(Lead lead) {
		return new LeadResponse(lead.getId(), lead.getFullName(), lead.getPhone(), lead.getEmail(), lead.getGoal(),
				lead.getLevel(), lead.getHeightCm(), lead.getWeightKg(), lead.getNote(), lead.getStatus(),
				lead.getCreatedAt());
	}
}
