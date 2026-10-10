package com.s2000coaching.domain;

public enum MemberStatus {

	/** Admin created the member; the e-mailed registration code has not been confirmed yet. */
	PENDING_VERIFICATION,

	/** Registration code confirmed — the member can sign in to the member area. */
	ACTIVE
}
