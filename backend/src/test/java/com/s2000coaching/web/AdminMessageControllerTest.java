package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.service.MessageService;
import com.s2000coaching.web.dto.MessageResponse;
import com.s2000coaching.web.dto.SendMessageRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AdminMessageController.class)
@TestPropertySource(properties = "app.admin.token=test-admin-token")
class AdminMessageControllerTest {

	private static final String ADMIN_HEADER = "X-Admin-Token";
	private static final String ADMIN_TOKEN = "test-admin-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MessageService messageService;

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
				.header(ADMIN_HEADER, ADMIN_TOKEN))
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
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new SendMessageRequest("nasıl geçti?"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.sender").isEqualTo("COACH");
	}
}
