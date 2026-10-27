package com.s2000coaching.service;

import java.util.UUID;

/**
 * Something that owns rows (and possibly files) belonging to a member and must remove them
 * when the member is deleted. Each feature that adds member-owned data implements this as a
 * bean, so {@code MemberService} discovers it instead of growing a constructor parameter for
 * every new table — and a new table cannot be forgotten in the deletion path by someone who
 * never opened that class.
 */
public interface MemberDataEraser {

	void eraseAllOf(UUID memberId);
}
