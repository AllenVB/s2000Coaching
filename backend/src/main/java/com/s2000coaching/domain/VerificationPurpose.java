package com.s2000coaching.domain;

public enum VerificationPurpose {

	/** One-time code mailed when an admin creates the member; consuming it activates the account. */
	REGISTRATION,

	/** One-time code mailed when an active member asks to sign in to the member area. */
	LOGIN
}
