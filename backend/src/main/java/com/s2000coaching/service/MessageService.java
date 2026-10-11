package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.repository.MessageRepository;
import com.s2000coaching.web.dto.MessageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One thread per member, two participants. Opening the thread marks the other side's messages
 * read, which is what clears the unread badges.
 */
@Service
public class MessageService {

	private final MessageRepository messageRepository;
	private final MemberService memberService;

	public MessageService(MessageRepository messageRepository, MemberService memberService) {
		this.messageRepository = messageRepository;
		this.memberService = memberService;
	}

	/** Read as the member: the coach's messages become read. */
	@Transactional
	public List<MessageResponse> readThreadAsMember(UUID memberId) {
		markRead(memberId, MessageSender.COACH);
		return thread(memberId);
	}

	/** Read as the coach: the member's messages become read. */
	@Transactional
	public List<MessageResponse> readThreadAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		markRead(memberId, MessageSender.MEMBER);
		return thread(memberId);
	}

	@Transactional
	public MessageResponse sendAsMember(Member member, String body) {
		return send(member, MessageSender.MEMBER, body);
	}

	@Transactional
	public MessageResponse sendAsCoach(UUID memberId, String body) {
		return send(memberService.requireMember(memberId), MessageSender.COACH, body);
	}

	/** How many of the coach's messages the member has not opened yet. */
	@Transactional(readOnly = true)
	public long countUnreadForMember(UUID memberId) {
		return messageRepository.countByMemberIdAndSenderAndReadAtIsNull(memberId, MessageSender.COACH);
	}

	@Transactional(readOnly = true)
	public List<UUID> memberIdsAwaitingReply() {
		return messageRepository.findMemberIdsWithUnreadMemberMessages();
	}

	private MessageResponse send(Member member, MessageSender sender, String body) {
		Message message = messageRepository.save(
				new Message(UUID.randomUUID(), member, sender, body.trim(), Instant.now()));
		return MessageResponse.from(message);
	}

	private List<MessageResponse> thread(UUID memberId) {
		return messageRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId).stream()
				.map(MessageResponse::from)
				.toList();
	}

	private void markRead(UUID memberId, MessageSender sender) {
		Instant now = Instant.now();
		messageRepository.findAllByMemberIdAndSenderAndReadAtIsNull(memberId, sender)
				.forEach(message -> message.markRead(now));
	}
}
