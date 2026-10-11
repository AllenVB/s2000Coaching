package com.s2000coaching.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrainingPlanResponse(
		UUID id,
		String name,
		String note,
		boolean template,
		boolean active,
		Instant updatedAt,
		List<PlanDayResponse> days) {
}
