package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MessageService;
import com.s2000coaching.web.dto.MessageResponse;
import com.s2000coaching.web.dto.SendMessageRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(MemberMessageController.class)
class MemberMessageControllerTest {

	private static final String BEARER = "Bearer token-123";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MessageService messageService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	private static Member member() {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		member.activate(Instant.now());
		return member;
	}

	@Test
	void shouldReturn401WithoutASession() {
		when(memberAuthService.requireMember(null)).thenThrow(new UnauthorizedException("Oturum bulunamadı."));

		assertThat(mvc.get().uri("/api/member/messages")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(messageService, never()).readThreadAsMember(any());
	}

	@Test
	void shouldReadTheThreadAsTheMember() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(messageService.readThreadAsMember(member.getId())).thenReturn(List.of(
				new MessageResponse(UUID.randomUUID(), MessageSender.COACH, "nasıl geçti?", Instant.now(),
						Instant.now())));

		assertThat(mvc.get().uri("/api/member/messages").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$[0].sender").isEqualTo("COACH");

		// Reading is what clears the badge, so the controller must go through the read path.
		verify(messageService).readThreadAsMember(member.getId());
	}

	@Test
	void shouldSendAsTheMember() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(messageService.sendAsMember(eq(member), eq("iyi geçti"))).thenReturn(
				new MessageResponse(UUID.randomUUID(), MessageSender.MEMBER, "iyi geçti", Instant.now(), null));

		assertThat(mvc.post().uri("/api/member/messages")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new SendMessageRequest("iyi geçti"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.sender").isEqualTo("MEMBER");
	}

	@Test
	void shouldReturn400ForAnEmptyMessage() {
		when(memberAuthService.requireMember(BEARER)).thenReturn(member());

		assertThat(mvc.post().uri("/api/member/messages")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new SendMessageRequest("   "))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(messageService, never()).sendAsMember(any(), any());
	}
}
