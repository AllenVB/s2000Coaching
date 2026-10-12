package com.s2000coaching.exception;

/** Request clashes with existing state, e.g. a member already registered with that e-mail. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
