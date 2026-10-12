package com.s2000coaching.exception;

/** Caller presented no credentials, or credentials that are no longer valid. */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
