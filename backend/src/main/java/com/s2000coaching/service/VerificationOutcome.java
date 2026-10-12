package com.s2000coaching.service;

/** Result of checking a submitted one-time code; mapped to an HTTP error by the callers. */
public enum VerificationOutcome {

	OK,
	NO_ACTIVE_CODE,
	EXPIRED,
	TOO_MANY_ATTEMPTS,
	MISMATCH
}
