package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CheckIn;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CheckInResponse(UUID id, LocalDate weekStart, int sleep, int stress, int energy, int hunger,
		Integer dietAdherencePercent, String note, String coachReply, Instant coachRepliedAt, Instant updatedAt) {

	public static CheckInResponse from(CheckIn checkIn) {
		return new CheckInResponse(checkIn.getId(), checkIn.getWeekStart(), checkIn.getSleep(), checkIn.getStress(),
				checkIn.getEnergy(), checkIn.getHunger(), checkIn.getDietAdherencePercent(), checkIn.getNote(),
				checkIn.getCoachReply(), checkIn.getCoachRepliedAt(), checkIn.getUpdatedAt());
	}
}
