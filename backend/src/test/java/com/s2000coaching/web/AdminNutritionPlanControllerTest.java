package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.NutritionPlanService;
import com.s2000coaching.web.dto.NutritionPlanResponse;
import com.s2000coaching.web.dto.UpsertNutritionPlanRequest;
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

@WebMvcTest(AdminNutritionPlanController.class)
@TestPropertySource(properties = "app.admin.token=test-admin-token")
class AdminNutritionPlanControllerTest {

	private static final String ADMIN_HEADER = "X-Admin-Token";
	private static final String ADMIN_TOKEN = "test-admin-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private NutritionPlanService nutritionPlanService;

	private static NutritionPlanResponse plan(String name, boolean template, boolean active) {
		return new NutritionPlanResponse(UUID.randomUUID(), name, null, template, active, 2400, 180, 250, 70,
				Instant.now(), List.of());
	}

	@Test
	void shouldRejectTemplatesWithoutTheAdminToken() {
		assertThat(mvc.get().uri("/api/admin/nutrition-plan-templates")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(nutritionPlanService, never()).listTemplates();
	}

	@Test
	void shouldCreateATemplate() {
		when(nutritionPlanService.createTemplate(any())).thenReturn(plan("2400 kcal", true, false));

		assertThat(mvc.post().uri("/api/admin/nutrition-plan-templates")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertNutritionPlanRequest("2400 kcal", null, 2400, 180, 250, 70))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.template").isEqualTo(true);
	}

	@Test
	void shouldReturn400ForAnImpossibleCalorieTarget() {
		assertThat(mvc.post().uri("/api/admin/nutrition-plan-templates")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertNutritionPlanRequest("Saçma", null, 50, null, null, null))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(nutritionPlanService, never()).createTemplate(any());
	}

	@Test
	void shouldPassTheTemplateIdThroughWhenAssigning() {
		UUID memberId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		when(nutritionPlanService.createForMember(eq(memberId), any(), eq(templateId)))
				.thenReturn(plan("Ekim", false, true));

		assertThat(mvc.post()
				.uri("/api/admin/members/" + memberId + "/nutrition-plans?templateId=" + templateId)
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertNutritionPlanRequest("Ekim", null, null, null, null, null))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.active").isEqualTo(true);
	}

	@Test
	void shouldReturn204WhenDeletingAPlan() {
		UUID planId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/nutrition-plans/" + planId).header(ADMIN_HEADER, ADMIN_TOKEN))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(nutritionPlanService).deletePlan(planId);
	}
}
