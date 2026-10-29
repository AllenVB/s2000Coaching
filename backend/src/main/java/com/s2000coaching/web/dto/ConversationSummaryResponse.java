package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MessageSender;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the coach's inbox: who the thread is with, how it currently stands, and enough
 * of the last message to recognise it without opening the thread.
 *
 * @param unreadFromMember messages the member sent that the coach has not opened
 * @param awaitingReply    true while the member is waiting — what sorts the inbox
 * @param lastMessageAt    null for a member the coach has never written to
 */
public record ConversationSummaryResponse(
		UUID memberId,
		String fullName,
		String email,
		MemberStatus status,
		long unreadFromMember,
		boolean awaitingReply,
		MessageSender lastSender,
		String lastMessagePreview,
		Instant lastMessageAt) {
}
