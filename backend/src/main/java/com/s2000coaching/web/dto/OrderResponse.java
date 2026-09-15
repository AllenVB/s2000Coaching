package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import java.time.Instant;

public record OrderResponse(
		String orderNumber,
		String packageName,
		long priceMinor,
		OrderStatus status,
		Instant createdAt) {

	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.getOrderNumber(),
				order.getCoachingPackage().getName(),
				order.getPriceMinor(),
				order.getStatus(),
				order.getCreatedAt());
	}
}
