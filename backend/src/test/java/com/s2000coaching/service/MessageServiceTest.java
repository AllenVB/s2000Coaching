package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MessageRepository;
import com.s2000coaching.repository.projection.MemberUnreadCount;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberService memberService;

	@Mock
	private MemberNotifier notifier;

	private MessageService messageService;
	private Member member;
	private Member other;

	@BeforeEach
	void setUp() {
		messageService = new MessageService(messageRepository, memberRepository, memberService, notifier);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
		other = new Member(UUID.randomUUID(), "baska@example.com", "Zeynep Kaya", "+905551112244", 168,
				new BigDecimal("61.00"), "yeni başlayan", MemberStatus.ACTIVE, Instant.now());
	}

	private Message message(MessageSender sender, String body) {
		return new Message(UUID.randomUUID(), member, sender, body, Instant.now());
	}

	@Test
	void shouldTrimAndStoreAMemberMessage() {
		when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = messageService.sendAsMember(member, "  omuzda ağrı var  ");

		assertThat(response.body()).isEqualTo("omuzda ağrı var");
		assertThat(response.sender()).isEqualTo(MessageSender.MEMBER);
		assertThat(response.readAt()).isNull();
	}

	@Test
	void shouldMarkOnlyTheOtherSidesMessagesRead() {
		Message fromCoach = message(MessageSender.COACH, "nasıl geçti?");
		when(messageRepository.findAllByMemberIdAndSenderAndReadAtIsNull(member.getId(), MessageSender.COACH))
				.thenReturn(List.of(fromCoach));
		when(messageRepository.findAllByMemberIdOrderByCreatedAtAsc(member.getId()))
				.thenReturn(List.of(fromCoach));

		var thread = messageService.readThreadAsMember(member.getId());

		assertThat(fromCoach.isUnread()).isFalse();
		assertThat(thread).singleElement().satisfies(m -> assertThat(m.readAt()).isNotNull());
	}

	@Test
	void shouldMarkTheMembersMessagesReadWhenTheCoachOpensTheThread() {
		Message fromMember = message(MessageSender.MEMBER, "iyi geçti");
		when(memberService.requireMember(member.getId())).thenReturn(member);
		when(messageRepository.findAllByMemberIdAndSenderAndReadAtIsNull(member.getId(), MessageSender.MEMBER))
				.thenReturn(List.of(fromMember));
		when(messageRepository.findAllByMemberIdOrderByCreatedAtAsc(member.getId()))
				.thenReturn(List.of(fromMember));

		messageService.readThreadAsCoach(member.getId());

		assertThat(fromMember.isUnread()).isFalse();
	}

	@Test
	void shouldKeepTheFirstReadTimestamp() {
		Message fromCoach = message(MessageSender.COACH, "nasıl geçti?");
		Instant first = Instant.now().minusSeconds(60);
		fromCoach.markRead(first);
		fromCoach.markRead(Instant.now());

		assertThat(fromCoach.getReadAt()).isEqualTo(first);
	}

	@Test
	void shouldListMembersWaitingForAReply() {
		UUID waiting = UUID.randomUUID();
		when(messageRepository.findMemberIdsWithUnreadMemberMessages()).thenReturn(List.of(waiting));

		assertThat(messageService.memberIdsAwaitingReply()).containsExactly(waiting);
	}

	@Test
	void shouldCountUnreadForTheMemberWithoutMarkingAnythingRead() {
		Message fromCoach = message(MessageSender.COACH, "nasıl geçti?");
		Message fromMember = message(MessageSender.MEMBER, "iyi");
		when(messageRepository.findAllByMemberIdOrderByCreatedAtAsc(member.getId()))
				.thenReturn(List.of(fromMember, fromCoach));

		var summary = messageService.unreadForMember(member.getId());

		assertThat(summary.unread()).isEqualTo(1);
		assertThat(summary.lastMessageAt()).isEqualTo(fromCoach.getCreatedAt());
		// Polling the badge must leave the thread untouched, or it would clear itself.
		assertThat(fromCoach.isUnread()).isTrue();
	}

	@Test
	void shouldPutMembersWaitingForAReplyAtTheTopOfTheInbox() {
		Message answered = new Message(UUID.randomUUID(), other, MessageSender.COACH, "plan hazır",
				Instant.now());
		Message waiting = new Message(UUID.randomUUID(), member, MessageSender.MEMBER, "dizim ağrıyor",
				Instant.now().minusSeconds(3600));
		when(memberRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(other, member));
		when(messageRepository.findLatestPerMember()).thenReturn(List.of(answered, waiting));
		when(messageRepository.countUnreadPerMember(MessageSender.MEMBER))
				.thenReturn(List.of(new MemberUnreadCount(member.getId(), 2L)));

		var conversations = messageService.conversations();

		// The member waiting comes first even though the other thread moved more recently.
		assertThat(conversations).extracting("memberId")
				.containsExactly(member.getId(), other.getId());
		assertThat(conversations.getFirst().unreadFromMember()).isEqualTo(2);
		assertThat(conversations.getFirst().awaitingReply()).isTrue();
		assertThat(conversations.getLast().awaitingReply()).isFalse();
	}

	@Test
	void shouldListAMemberWithNoMessagesLastSoAThreadCanStillBeStarted() {
		Message onlyThread = new Message(UUID.randomUUID(), member, MessageSender.COACH, "merhaba",
				Instant.now());
		when(memberRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(other, member));
		when(messageRepository.findLatestPerMember()).thenReturn(List.of(onlyThread));
		when(messageRepository.countUnreadPerMember(MessageSender.MEMBER)).thenReturn(List.of());

		var conversations = messageService.conversations();

		assertThat(conversations.getLast().memberId()).isEqualTo(other.getId());
		assertThat(conversations.getLast().lastMessageAt()).isNull();
		assertThat(conversations.getLast().lastMessagePreview()).isNull();
	}

	@Test
	void shouldFlattenAndShortenTheInboxPreview() {
		String longBody = "ilk satır\nikinci  satır " + "x".repeat(200);
		when(memberRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(member));
		when(messageRepository.findLatestPerMember()).thenReturn(
				List.of(new Message(UUID.randomUUID(), member, MessageSender.MEMBER, longBody, Instant.now())));
		when(messageRepository.countUnreadPerMember(MessageSender.MEMBER)).thenReturn(List.of());

		String preview = messageService.conversations().getFirst().lastMessagePreview();

		assertThat(preview).startsWith("ilk satır ikinci satır").hasSize(141).endsWith("…");
	}

	@Test
	void shouldCountEveryUnansweredMessageForTheCoachBadge() {
		when(messageRepository.countBySenderAndReadAtIsNull(MessageSender.MEMBER)).thenReturn(4L);

		assertThat(messageService.unreadForCoach().unread()).isEqualTo(4);
	}
}
