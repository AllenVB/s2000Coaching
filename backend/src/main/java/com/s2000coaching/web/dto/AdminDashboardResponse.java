package com.s2000coaching.web.dto;

import java.util.List;

/**
 * Answers the one question the management page opens with: who needs attention today.
 *
 * @param pendingMembers members whose registration code is still unconfirmed
 * @param staleMembers   active members with no check-in in the last {@code staleAfterDays} days
 * @param awaitingReply  members whose last message to the coach is still unread
 */
public record AdminDashboardResponse(
		long totalMembers,
		long activeMembers,
		int staleAfterDays,
		List<MemberResponse> pendingMembers,
		List<MemberResponse> staleMembers,
		List<MemberResponse> awaitingReply,
		List<OrderSummaryResponse> recentOrders) {
}
