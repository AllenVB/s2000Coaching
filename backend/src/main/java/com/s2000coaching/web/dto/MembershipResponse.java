package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Membership;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param daysLeft days until the period ends, counting today; negative once it has ended
 */
public record MembershipResponse(UUID id, String label, LocalDate startsOn, LocalDate endsOn, long daysLeft,
		boolean active, String orderNumber) {

	public static MembershipResponse from(Membership membership, LocalDate today) {
		long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, membership.getEndsOn());
		return new MembershipResponse(membership.getId(), membership.getLabel(), membership.getStartsOn(),
				membership.getEndsOn(), daysLeft, !membership.getEndsOn().isBefore(today)
						&& !membership.getStartsOn().isAfter(today),
				membership.getOrder() == null ? null : membership.getOrder().getOrderNumber());
	}
}
