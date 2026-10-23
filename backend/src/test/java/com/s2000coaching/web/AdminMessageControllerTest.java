package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.service.MessageService;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.web.dto.ConversationSummaryResponse;
import com.s2000coaching.web.dto.MessageResponse;
import com.s2000coaching.web.dto.UnreadSummaryResponse;
import com.s2000coaching.web.dto.SendMessageRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AdminMessageController.class)
class AdminMessageControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MessageService messageService;

	/** The /api/admin/** interceptor resolves the coach session through this. */
	@MockitoBean
	private CoachAuthService coachAuthService;

	@BeforeEach
	void stubCoachSession() {
		// The interceptor only cares whether this throws, so the valid token resolves to
		// nothing in particular and everything else is rejected — as the real one does.
		// doThrow/doReturn rather than when(...): a second when() would itself call the
		// already-stubbed method and trip the exception during setup.
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(BEARER);
	}

	@Test
	void shouldRejectTheThreadWithoutTheAdminToken() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/messages"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(messageService, never()).readThreadAsCoach(any());
	}

	@Test
	void shouldReadTheThreadAsTheCoach() {
		UUID memberId = UUID.randomUUID();
		when(messageService.readThreadAsCoach(memberId)).thenReturn(List.of(
				new MessageResponse(UUID.randomUUID(), MessageSender.MEMBER, "iyi geçti", Instant.now(),
						Instant.now())));

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/messages")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$[0].body").isEqualTo("iyi geçti");

		verify(messageService).readThreadAsCoach(memberId);
	}

	@Test
	void shouldSendAsTheCoach() {
		UUID memberId = UUID.randomUUID();
		when(messageService.sendAsCoach(eq(memberId), eq("nasıl geçti?"))).thenReturn(
				new MessageResponse(UUID.randomUUID(), MessageSender.COACH, "nasıl geçti?", Instant.now(),
						null));

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/messages")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new SendMessageRequest("nasıl geçti?"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.sender").isEqualTo("COACH");
	}

	@Test
	void shouldListTheInboxWithoutTouchingAnyThread() {
		UUID memberId = UUID.randomUUID();
		when(messageService.conversations()).thenReturn(List.of(new ConversationSummaryResponse(memberId,
				"Ahmet Yılmaz", "sporcu@example.com", MemberStatus.ACTIVE, 2, true, MessageSender.MEMBER,
				"dizim ağrıyor", Instant.now())));

		assertThat(mvc.get().uri("/api/admin/messages/threads").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$[0].unreadFromMember").isEqualTo(2);

		verify(messageService, never()).readThreadAsCoach(any());
	}

	@Test
	void shouldReportTheCoachUnreadCount() {
		when(messageService.unreadForCoach()).thenReturn(new UnreadSummaryResponse(3, null));

		assertThat(mvc.get().uri("/api/admin/messages/unread").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.unread").isEqualTo(3);
	}

	@Test
	void shouldRejectTheInboxWithoutTheAdminToken() {
		assertThat(mvc.get().uri("/api/admin/messages/threads")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(messageService, never()).conversations();
	}
}
