package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.CoachNoteService;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.CoachNoteResponse;
import com.s2000coaching.web.dto.UpsertCoachNoteRequest;
import java.time.Instant;
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

@WebMvcTest(AdminCoachNoteController.class)
class AdminCoachNoteControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private CoachNoteService coachNoteService;

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
				.header(HttpHeaders.AUTHORIZATION, BEARER)
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
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCoachNoteRequest(" "))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(coachNoteService, never()).addNote(any(), any());
	}

	@Test
	void shouldReturn204WhenDeletingANote() {
		UUID noteId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/notes/" + noteId).header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(coachNoteService).deleteNote(noteId);
	}
}
