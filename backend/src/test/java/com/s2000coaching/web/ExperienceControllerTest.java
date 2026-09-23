package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
	void shouldCreateCategory() {
		var response = new ExperienceCategoryResponse(UUID.randomUUID(), "Yoga", List.of());
		when(experienceService.createCategory(eq("Yoga"))).thenReturn(response);

		assertThat(mvc.post().uri("/api/experience-categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCategoryRequest("Yoga"))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.name").isEqualTo("Yoga");
	}

	@Test
	void shouldReturn400WhenCategoryNameIsBlank() {
		assertThat(mvc.post().uri("/api/experience-categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertCategoryRequest(""))))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void shouldReturn204WhenDeletingItem() {
		UUID itemId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/experience-items/" + itemId))
				.hasStatus(HttpStatus.NO_CONTENT);
	}
}
