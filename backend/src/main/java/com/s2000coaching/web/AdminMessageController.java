package com.s2000coaching.web;

import com.s2000coaching.service.MessageService;
import com.s2000coaching.web.dto.ConversationSummaryResponse;
import com.s2000coaching.web.dto.MessageResponse;
import com.s2000coaching.web.dto.SendMessageRequest;
import com.s2000coaching.web.dto.UnreadSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminMessageController {

	private final MessageService messageService;

	public AdminMessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	/** The inbox list. Reading it must leave every thread as it was, so nothing is marked. */
	@GetMapping("/api/admin/messages/threads")
	public List<ConversationSummaryResponse> conversations() {
		return messageService.conversations();
	}

	/** Polled for the panel's badge, so it marks nothing read either. */
	@GetMapping("/api/admin/messages/unread")
	public UnreadSummaryResponse unread() {
		return messageService.unreadForCoach();
	}

	/** Reading the thread marks the member's messages read, clearing the reply badge. */
	@GetMapping("/api/admin/members/{memberId}/messages")
	public List<MessageResponse> thread(@PathVariable UUID memberId) {
		return messageService.readThreadAsCoach(memberId);
	}

	@PostMapping("/api/admin/members/{memberId}/messages")
	public MessageResponse send(@PathVariable UUID memberId,
			@Valid @RequestBody SendMessageRequest request) {
		return messageService.sendAsCoach(memberId, request.body());
	}
}
