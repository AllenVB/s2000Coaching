package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MessageService;
import com.s2000coaching.web.dto.MessageResponse;
import com.s2000coaching.web.dto.SendMessageRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The member's thread with their coach. */
@RestController
public class MemberMessageController {

	private final MessageService messageService;
	private final MemberAuthService memberAuthService;

	public MemberMessageController(MessageService messageService, MemberAuthService memberAuthService) {
		this.messageService = messageService;
		this.memberAuthService = memberAuthService;
	}

	/** Reading the thread marks the coach's messages read. */
	@GetMapping("/api/member/messages")
	public List<MessageResponse> thread(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return messageService.readThreadAsMember(member.getId());
	}

	@PostMapping("/api/member/messages")
	public MessageResponse send(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody SendMessageRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return messageService.sendAsMember(member, request.body());
	}
}
