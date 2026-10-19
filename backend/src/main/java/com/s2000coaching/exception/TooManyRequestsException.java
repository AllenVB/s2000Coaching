package com.s2000coaching.exception;

/**
 * The caller is asking for something faster, or more often, than the endpoint allows —
 * currently only new verification codes.
 */
public class TooManyRequestsException extends RuntimeException {

	public TooManyRequestsException(String message) {
		super(message);
	}
}
