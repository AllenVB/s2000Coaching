package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, MessageSender sender, String body, Instant createdAt, Instant readAt) {

	public static MessageResponse from(Message message) {
		return new MessageResponse(message.getId(), message.getSender(), message.getBody(),
				message.getCreatedAt(), message.getReadAt());
	}
}
