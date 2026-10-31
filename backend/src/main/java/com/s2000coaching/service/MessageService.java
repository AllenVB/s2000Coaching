package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MessageRepository;
import com.s2000coaching.repository.projection.MemberUnreadCount;
import com.s2000coaching.web.dto.ConversationSummaryResponse;
import com.s2000coaching.web.dto.MessageResponse;
import com.s2000coaching.web.dto.UnreadSummaryResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One thread per member, two participants. Opening the thread marks the other side's messages
 * read, which is what clears the unread badges — so the badge and inbox queries deliberately
 * never load a thread.
 */
@Service
public class MessageService {

	/** Enough of the last message to recognise the thread in the inbox list. */
	private static final int PREVIEW_LENGTH = 140;

	private final MessageRepository messageRepository;
	private final MemberRepository memberRepository;
	private final MemberService memberService;
	private final MemberNotifier memberNotifier;

	public MessageService(MessageRepository messageRepository, MemberRepository memberRepository,
			MemberService memberService, MemberNotifier memberNotifier) {
		this.memberNotifier = memberNotifier;
		this.messageRepository = messageRepository;
		this.memberRepository = memberRepository;
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
		Member member = memberService.requireMember(memberId);
		MessageResponse sent = send(member, MessageSender.COACH, body);
		memberNotifier.coachReplied(member, "mesajınıza", "/uye/mesajlar");
		return sent;
	}

	/** How many of the coach's messages the member has not opened yet. */
	@Transactional(readOnly = true)
	public long countUnreadForMember(UUID memberId) {
		return messageRepository.countByMemberIdAndSenderAndReadAtIsNull(memberId, MessageSender.COACH);
	}

	/** The member's badge: polled, so it must not mark the coach's messages read. */
	@Transactional(readOnly = true)
	public UnreadSummaryResponse unreadForMember(UUID memberId) {
		List<Message> messages = messageRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId);
		long unread = messages.stream()
				.filter(message -> message.getSender() == MessageSender.COACH && message.isUnread())
				.count();
		Instant lastMessageAt = messages.isEmpty() ? null : messages.getLast().getCreatedAt();
		return new UnreadSummaryResponse(unread, lastMessageAt);
	}

	/** The coach's badge: every member message still unopened, across all threads. */
	@Transactional(readOnly = true)
	public UnreadSummaryResponse unreadForCoach() {
		long unread = messageRepository.countBySenderAndReadAtIsNull(MessageSender.MEMBER);
		return new UnreadSummaryResponse(unread, null);
	}

	/**
	 * The coach's inbox: every member, so a thread can be started with someone who has never
	 * written. Members waiting on a reply come first, then the threads that moved most
	 * recently, then everyone else by name.
	 */
	@Transactional(readOnly = true)
	public List<ConversationSummaryResponse> conversations() {
		Map<UUID, Message> latest = new HashMap<>();
		for (Message message : messageRepository.findLatestPerMember()) {
			latest.putIfAbsent(message.getMember().getId(), message);
		}
		Map<UUID, Long> unread = messageRepository.countUnreadPerMember(MessageSender.MEMBER).stream()
				.collect(Collectors.toMap(MemberUnreadCount::memberId, MemberUnreadCount::count));

		return memberRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(member -> summarise(member, latest.get(member.getId()),
						unread.getOrDefault(member.getId(), 0L)))
				.sorted(inboxOrder())
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UUID> memberIdsAwaitingReply() {
		return messageRepository.findMemberIdsWithUnreadMemberMessages();
	}

	private ConversationSummaryResponse summarise(Member member, Message last, long unread) {
		return new ConversationSummaryResponse(
				member.getId(),
				member.getFullName(),
				member.getEmail(),
				member.getStatus(),
				unread,
				unread > 0,
				last == null ? null : last.getSender(),
				last == null ? null : preview(last.getBody()),
				last == null ? null : last.getCreatedAt());
	}

	private static Comparator<ConversationSummaryResponse> inboxOrder() {
		Comparator<ConversationSummaryResponse> waitingFirst =
				Comparator.comparing(ConversationSummaryResponse::awaitingReply).reversed();
		// A thread that has never moved sorts last rather than first, hence the null handling.
		Comparator<ConversationSummaryResponse> newestFirst = Comparator.comparing(
				ConversationSummaryResponse::lastMessageAt,
				Comparator.nullsLast(Comparator.reverseOrder()));
		return waitingFirst.thenComparing(newestFirst)
				.thenComparing(ConversationSummaryResponse::fullName, String.CASE_INSENSITIVE_ORDER);
	}

	private static String preview(String body) {
		String flattened = body.replaceAll("\\s+", " ").trim();
		return flattened.length() <= PREVIEW_LENGTH ? flattened
				: flattened.substring(0, PREVIEW_LENGTH).trim() + "…";
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
