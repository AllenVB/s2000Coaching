package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;

/**
 * Seam for charging an order. {@link MockPaymentGateway} is the only implementation today;
 * a real provider (e.g. iyzico) can be added later as another {@code @Service} bean without
 * touching the order service or controllers.
 */
public interface PaymentGateway {

	PaymentResult charge(Order order);
}
