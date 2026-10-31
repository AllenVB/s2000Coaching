package com.s2000coaching.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.mail.NotificationMailSender;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberNotifierTest {

	@Mock
	private NotificationMailSender mailSender;

	private MemberNotifier notifier;
	private Member member;

	@BeforeEach
	void setUp() {
		notifier = new MemberNotifier(mailSender, "https://s2000.example/");
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet Yılmaz", "", 180, new BigDecimal("80"), "x",
				MemberStatus.ACTIVE, Instant.now());
	}

	@Test
	void shouldTellTheMemberThereIsAnAnswerWithoutQuotingIt() {
		when(mailSender.delivers()).thenReturn(true);

		notifier.coachReplied(member, "mesajınıza", "/uye/mesajlar");

		verify(mailSender).send(eq("a@example.com"), eq("Koçunuz yanıt verdi"),
				contains("https://s2000.example/uye/mesajlar"));
	}

	@Test
	void shouldStaySilentWhenRemindersAreOffOrNothingDelivers() {
		when(mailSender.delivers()).thenReturn(false);
		notifier.coachReplied(member, "mesajınıza", "/uye/mesajlar");

		member.setRemindersEnabled(false);
		notifier.coachReplied(member, "mesajınıza", "/uye/mesajlar");

		verify(mailSender, never()).send(any(), any(), any());
	}
}
