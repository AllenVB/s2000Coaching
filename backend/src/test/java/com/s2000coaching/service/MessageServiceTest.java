package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.repository.MessageRepository;
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
	private MemberService memberService;

	private MessageService messageService;
	private Member member;

	@BeforeEach
	void setUp() {
		messageService = new MessageService(messageRepository, memberService);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
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
}
