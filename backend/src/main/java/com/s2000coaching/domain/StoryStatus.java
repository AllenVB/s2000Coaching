package com.s2000coaching.domain;

/**
 * Where a success story is in its life. The order is the point: nothing reaches {@code PUBLISHED}
 * without passing {@code CONSENTED}, which only the member can set.
 */
public enum StoryStatus {
	/** Being written by the coach; the member has not been asked. */
	DRAFT,
	/** Sent to the member, who has not answered yet. */
	AWAITING_CONSENT,
	/** The member agreed to this exact content; the coach may publish it. */
	CONSENTED,
	/** Visible on the public site. */
	PUBLISHED,
	/** The member said no. */
	DECLINED,
	/** The member withdrew an earlier consent; the photo copies are gone. */
	REVOKED
}
