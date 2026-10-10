package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.CoachNoteService;
import com.s2000coaching.web.dto.CoachNoteResponse;
import com.s2000coaching.web.dto.UpsertCoachNoteRequest;
import java.time.Instant;
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

@WebMvcTest(AdminCoachNoteController.class)
@TestPropertySource(properties = "app.admin.token=test-admin-token")
class AdminCoachNoteControllerTest {

	private static final String ADMIN_HEADER = "X-Admin-Token";
	private static final String ADMIN_TOKEN = "test-admin-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private CoachNoteService coachNoteService;

	@Test
	void shouldRejectNotesWithoutTheAdminToken() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/notes"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(coachNoteService, never()).listNotes(any());
	}

	@Test
	void shouldCreateANote() {
		UUID memberId = UUID.randomUUID();
		Instant now = Instant.now();
		when(coachNoteService.addNote(eq(memberId), eq("Omuz ağrısı")))
				.thenReturn(new CoachNoteResponse(UUID.randomUUID(), "Omuz ağrısı", now, now));

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/notes")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCoachNoteRequest("Omuz ağrısı"))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.body").isEqualTo("Omuz ağrısı");
	}

	@Test
	void shouldReturn400ForAnEmptyNote() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/notes")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCoachNoteRequest(" "))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(coachNoteService, never()).addNote(any(), any());
	}

	@Test
	void shouldReturn204WhenDeletingANote() {
		UUID noteId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/notes/" + noteId).header(ADMIN_HEADER, ADMIN_TOKEN))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(coachNoteService).deleteNote(noteId);
	}
}
