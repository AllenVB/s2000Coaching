package com.s2000coaching.exception;

/**
 * The password was right but the e-mail address was never confirmed. Answered as 403 so the
 * sign-in page can send the member on to the code step instead of showing a failure.
 */
public class EmailNotVerifiedException extends RuntimeException {

	public EmailNotVerifiedException(String message) {
		super(message);
	}
}
