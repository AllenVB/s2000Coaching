package com.s2000coaching.repository.projection;

import java.util.UUID;

/** How many messages one member's thread still has unread, for one direction. */
public record MemberUnreadCount(UUID memberId, long count) {
}
