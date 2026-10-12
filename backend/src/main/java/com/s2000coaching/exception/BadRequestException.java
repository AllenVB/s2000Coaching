package com.s2000coaching.exception;

/** Request is well-formed but semantically rejected, e.g. a wrong or expired verification code. */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}
}
