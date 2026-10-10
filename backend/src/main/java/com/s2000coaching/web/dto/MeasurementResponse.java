package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Measurement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MeasurementResponse(
		UUID id,
		LocalDate measuredOn,
		BigDecimal weightKg,
		BigDecimal waistCm,
		BigDecimal chestCm,
		BigDecimal hipCm,
		BigDecimal armCm,
		BigDecimal thighCm,
		String note,
		Instant updatedAt) {

	public static MeasurementResponse from(Measurement measurement) {
		return new MeasurementResponse(
				measurement.getId(),
				measurement.getMeasuredOn(),
				measurement.getWeightKg(),
				measurement.getWaistCm(),
				measurement.getChestCm(),
				measurement.getHipCm(),
				measurement.getArmCm(),
				measurement.getThighCm(),
				measurement.getNote(),
				measurement.getUpdatedAt());
	}
}
