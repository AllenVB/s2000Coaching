package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PaymentMethod;
import java.time.Instant;
import java.util.UUID;

/** @param memberId the member this order belongs to, or null if the buyer is not one yet */
public record OrderSummaryResponse(
		String orderNumber,
		String customerName,
		String email,
		String phone,
		String cityAge,
		String packageName,
		long priceMinor,
		OrderStatus status,
		Instant createdAt,
		UUID memberId,
		PaymentMethod paymentMethod,
		Instant paidAt) {

	public static OrderSummaryResponse from(Order order) {
		return new OrderSummaryResponse(
				order.getOrderNumber(),
				order.getCustomerName(),
				order.getEmail(),
				order.getPhone(),
				order.getCityAge(),
				order.getCoachingPackage().getName(),
				order.getPriceMinor(),
				order.getStatus(),
				order.getCreatedAt(),
				order.getMember() == null ? null : order.getMember().getId(),
				order.getPaymentMethod(),
				order.getPaidAt());
	}
}
