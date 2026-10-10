package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import java.time.Instant;

public record OrderSummaryResponse(
		String orderNumber,
		String customerName,
		String email,
		String phone,
		String packageName,
		long priceMinor,
		OrderStatus status,
		Instant createdAt) {

	public static OrderSummaryResponse from(Order order) {
		return new OrderSummaryResponse(
				order.getOrderNumber(),
				order.getCustomerName(),
				order.getEmail(),
				order.getPhone(),
				order.getCoachingPackage().getName(),
				order.getPriceMinor(),
				order.getStatus(),
				order.getCreatedAt());
	}
}
