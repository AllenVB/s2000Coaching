package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PaymentMethod;
import com.s2000coaching.payment.BankDetails;
import java.time.Instant;

public record OrderResponse(
		String orderNumber,
		String packageName,
		long priceMinor,
		OrderStatus status,
		Instant createdAt,
		PaymentMethod paymentMethod,
		/** Where to send the buyer to pay, when the provider has its own page; otherwise null. */
		String redirectUrl,
		/** Set for an IBAN order: where to transfer and what to write in the description. */
		BankDetails bankTransfer) {

	public static OrderResponse from(Order order, String redirectUrl, BankDetails bankTransfer) {
		return new OrderResponse(
				order.getOrderNumber(),
				order.getCoachingPackage().getName(),
				order.getPriceMinor(),
				order.getStatus(),
				order.getCreatedAt(),
				order.getPaymentMethod(),
				redirectUrl,
				bankTransfer);
	}
}
