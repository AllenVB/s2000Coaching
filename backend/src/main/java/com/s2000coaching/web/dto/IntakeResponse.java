package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MemberIntake;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IntakeResponse(UUID id, LocalDate consumedOn, int calories, Integer proteinG, Integer carbsG,
		Integer fatG, Integer sodiumMg, String note, Instant updatedAt) {

	public static IntakeResponse from(MemberIntake intake) {
		return new IntakeResponse(intake.getId(), intake.getConsumedOn(), intake.getCalories(),
				intake.getProteinG(), intake.getCarbsG(), intake.getFatG(), intake.getSodiumMg(),
				intake.getNote(), intake.getUpdatedAt());
	}
}
