package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.s2000coaching.exception.ForbiddenException;
import com.s2000coaching.service.ExperienceService;
import com.s2000coaching.web.dto.ExperienceCategoryResponse;
import com.s2000coaching.web.dto.UpsertCategoryRequest;
import java.util.List;
import java.util.UUID;
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
	void shouldCreateCategoryWhenTokenHeaderIsValid() {
		var response = new ExperienceCategoryResponse(UUID.randomUUID(), "Yoga", List.of());
		when(experienceService.createCategory(eq("Yoga"), eq("right-token"))).thenReturn(response);

		assertThat(mvc.post().uri("/api/experience-categories")
				.header("X-Edit-Token", "right-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCategoryRequest("Yoga"))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.name").isEqualTo("Yoga");
	}

	@Test
	void shouldReturn403WhenCreatingCategoryWithoutToken() {
		when(experienceService.createCategory(eq("Yoga"), isNull()))
				.thenThrow(new ForbiddenException("Düzenleme parolası geçersiz"));

		assertThat(mvc.post().uri("/api/experience-categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCategoryRequest("Yoga"))))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldReturn400WhenCategoryNameIsBlank() {
		assertThat(mvc.post().uri("/api/experience-categories")
				.header("X-Edit-Token", "right-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCategoryRequest(""))))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void shouldReturn204WhenDeletingItemWithValidToken() {
		UUID itemId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/experience-items/" + itemId).header("X-Edit-Token", "right-token"))
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void shouldReturn403WhenDeletingItemWithoutToken() {
		UUID itemId = UUID.randomUUID();
		doThrow(new ForbiddenException("Düzenleme parolası geçersiz"))
				.when(experienceService).deleteItem(eq(itemId), isNull());

		assertThat(mvc.delete().uri("/api/experience-items/" + itemId))
				.hasStatus(HttpStatus.FORBIDDEN);
	}
}
