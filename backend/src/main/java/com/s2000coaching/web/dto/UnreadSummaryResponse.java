package com.s2000coaching.web.dto;

import java.time.Instant;

/**
 * The badge feed. Polled on a timer, so unlike reading the thread it must never mark
 * anything read.
 *
 * @param unread           messages from the other side still unopened
 * @param lastMessageAt    when the thread last moved, null while it is empty
 */
public record UnreadSummaryResponse(long unread, Instant lastMessageAt) {
}
