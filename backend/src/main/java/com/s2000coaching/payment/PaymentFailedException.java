package com.s2000coaching.payment;

/** The card provider could not be reached or refused the request. The message is safe to log. */
public class PaymentFailedException extends RuntimeException {

	public PaymentFailedException(String message) {
		super(message);
	}

	public PaymentFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
