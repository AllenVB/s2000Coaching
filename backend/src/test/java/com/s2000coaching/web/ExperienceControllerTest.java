package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.exception.ForbiddenException;
import com.s2000coaching.service.ExperienceService;
import com.s2000coaching.web.dto.ExperienceNoteResponse;
import com.s2000coaching.web.dto.UpdateExperienceRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ExperienceController.class)
class ExperienceControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private ExperienceService experienceService;

	@Test
	void shouldUpdateAndReturnNoteWhenTokenHeaderIsValid() {
		var response = new ExperienceNoteResponse(ExperienceCategory.ANTRENMAN, "güncel içerik", Instant.now());
		when(experienceService.update(eq(ExperienceCategory.ANTRENMAN), any(), eq("right-token")))
				.thenReturn(response);

		assertThat(mvc.put().uri("/api/experiences/ANTRENMAN")
				.header("X-Edit-Token", "right-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpdateExperienceRequest("güncel içerik"))))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content").isEqualTo("güncel içerik");
	}

	@Test
	void shouldReturn403WhenTokenHeaderIsMissing() {
		when(experienceService.update(eq(ExperienceCategory.ANTRENMAN), any(), isNull()))
				.thenThrow(new ForbiddenException("Düzenleme parolası geçersiz"));

		assertThat(mvc.put().uri("/api/experiences/ANTRENMAN")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpdateExperienceRequest("x"))))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldReturn400WhenContentIsBlankPayloadMissing() {
		assertThat(mvc.put().uri("/api/experiences/ANTRENMAN")
				.header("X-Edit-Token", "right-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}
}
