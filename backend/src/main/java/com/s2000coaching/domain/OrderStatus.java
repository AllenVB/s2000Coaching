package com.s2000coaching.domain;

public enum OrderStatus {
	/** Created, no payment step has been started. */
	PENDING,
	/** Bank transfer chosen; the coach confirms it once the money has arrived. */
	AWAITING_TRANSFER,
	/** Sent to the card provider's page; settled by its callback. */
	AWAITING_PAYMENT,
	/** Money received and confirmed — this, and only this, grants a membership. */
	PAID,
	/** Settled by the development gateway: nothing was actually paid. */
	MOCK_PAID,
	FAILED,
	CANCELLED
}
