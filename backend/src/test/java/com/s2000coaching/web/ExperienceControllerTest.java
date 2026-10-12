package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.ExperienceService;
import com.s2000coaching.web.dto.ExperienceCategoryResponse;
import com.s2000coaching.web.dto.ExperienceNoteResponse;
import com.s2000coaching.web.dto.ExperienceTopicResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ExperienceController.class)
class ExperienceControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ExperienceService experienceService;

	@Test
	void shouldReturnCategoryTopicNoteTree() {
		var note = new ExperienceNoteResponse(UUID.randomUUID(), "1,2-2 g/kg yeterlidir.");
		var topic = new ExperienceTopicResponse(UUID.randomUUID(), "Protein", List.of(note));
		var category = new ExperienceCategoryResponse(UUID.randomUUID(), "Beslenme", List.of(topic));
		when(experienceService.listCategories()).thenReturn(List.of(category));

		assertThat(mvc.get().uri("/api/experience-categories"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$[0].topics[0].notes[0].content").isEqualTo("1,2-2 g/kg yeterlidir.");
	}
}
