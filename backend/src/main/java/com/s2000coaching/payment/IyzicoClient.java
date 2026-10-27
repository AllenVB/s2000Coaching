package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;
import java.math.BigDecimal;

/**
 * The two calls the card flow needs from iyzico, behind an interface so the rules around them
 * (what counts as a successful payment) can be tested without the provider.
 */
public interface IyzicoClient {

	/** Whether the API keys and the callback address are set. */
	boolean configured();

	/** Opens a hosted payment page for the order. */
	Initialized initialize(Order order, String buyerIp);

	/**
	 * Asks iyzico, server to server, what became of a payment token. The browser's own claim
	 * that it paid is never trusted: the callback only carries the token, and the answer comes
	 * from here.
	 */
	Result retrieve(String token);

	record Initialized(String token, String paymentPageUrl) {
	}

	/**
	 * @param success    the provider says the payment went through and its answer is authentic
	 * @param basketId   the order number the payment was opened for
	 * @param paidPrice  what was actually charged
	 */
	record Result(boolean success, String basketId, BigDecimal paidPrice, String errorMessage) {
	}
}
