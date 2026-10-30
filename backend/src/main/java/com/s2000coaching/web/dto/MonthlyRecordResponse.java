package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MonthlyRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MonthlyRecordResponse(
		UUID id,
		String period,
		BigDecimal heightCm,
		BigDecimal weightKg,
		BigDecimal shoulderCm,
		BigDecimal chestCm,
		BigDecimal armCm,
		BigDecimal neckCm,
		BigDecimal waistCm,
		BigDecimal hipCm,
		BigDecimal thighCm,
		Instant updatedAt) {

	public static MonthlyRecordResponse from(MonthlyRecord record) {
		return new MonthlyRecordResponse(record.getId(), record.getPeriod(), record.getHeightCm(),
				record.getWeightKg(), record.getShoulderCm(), record.getChestCm(), record.getArmCm(),
				record.getNeckCm(), record.getWaistCm(), record.getHipCm(), record.getThighCm(),
				record.getUpdatedAt());
	}
}
