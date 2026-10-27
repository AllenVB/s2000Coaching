package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.PaymentMethod;

/**
 * One way of paying for an order. Each method is its own {@code @Service}; {@link
 * PaymentMethods} decides which of them a buyer is offered, so adding a provider never touches
 * the order service or the controllers.
 */
public interface PaymentGateway {

	PaymentMethod method();

	/** Whether this method is configured well enough to offer. */
	boolean enabled();

	/** Starts payment for the order and says what state it is now in. */
	PaymentInitiation initiate(Order order, PaymentContext context);
}
