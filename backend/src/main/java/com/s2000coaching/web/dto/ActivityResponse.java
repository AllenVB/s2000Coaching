package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MemberActivity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityResponse(UUID id, LocalDate performedOn, Integer steps, Integer cardioMinutes,
		Instant updatedAt) {

	public static ActivityResponse from(MemberActivity activity) {
		return new ActivityResponse(activity.getId(), activity.getPerformedOn(), activity.getSteps(),
				activity.getCardioMinutes(), activity.getUpdatedAt());
	}
}
