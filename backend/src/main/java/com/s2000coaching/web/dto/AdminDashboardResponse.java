package com.s2000coaching.web.dto;

import java.util.List;

/**
 * Answers the one question the management page opens with: who needs attention today.
 *
 * @param pendingMembers members whose registration code is still unconfirmed
 */
public record AdminDashboardResponse(
		long totalMembers,
		long activeMembers,
		List<MemberResponse> pendingMembers,
		List<OrderSummaryResponse> recentOrders) {
}
