package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.NutritionPlanService;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.NutritionPlanResponse;
import com.s2000coaching.web.dto.UpsertMealItemRequest;
import com.s2000coaching.web.dto.UpsertNutritionPlanRequest;
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

@WebMvcTest(AdminNutritionPlanController.class)
class AdminNutritionPlanControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private NutritionPlanService nutritionPlanService;

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

	private static NutritionPlanResponse plan(String name, boolean template, boolean active) {
		return new NutritionPlanResponse(UUID.randomUUID(), name, null, template, active, 2400, 180, 250, 70,
				null, Instant.now(), List.of());
	}

	@Test
	void shouldRejectTemplatesWithoutTheAdminToken() {
		assertThat(mvc.get().uri("/api/admin/nutrition-plan-templates")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(nutritionPlanService, never()).listTemplates();
	}

	@Test
	void shouldCreateATemplate() {
		when(nutritionPlanService.createTemplate(any(), anyBoolean())).thenReturn(plan("2400 kcal", true, false));

		assertThat(mvc.post().uri("/api/admin/nutrition-plan-templates")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertNutritionPlanRequest("2400 kcal", null, 2400, 180, 250, 70, null))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.template").isEqualTo(true);
	}

	@Test
	void shouldReturn400ForAnImpossibleCalorieTarget() {
		assertThat(mvc.post().uri("/api/admin/nutrition-plan-templates")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertNutritionPlanRequest("Saçma", null, 50, null, null, null, null))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(nutritionPlanService, never()).createTemplate(any(), anyBoolean());
	}

	@Test
	void shouldPassTheTemplateIdThroughWhenAssigning() {
		UUID memberId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		when(nutritionPlanService.createForMember(eq(memberId), any(), eq(templateId), eq(false)))
				.thenReturn(plan("Ekim", false, true));

		assertThat(mvc.post()
				.uri("/api/admin/members/" + memberId + "/nutrition-plans?templateId=" + templateId)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertNutritionPlanRequest("Ekim", null, null, null, null, null, null))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.active").isEqualTo(true);
	}

	@Test
	void shouldReturn204WhenDeletingAPlan() {
		UUID planId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/nutrition-plans/" + planId).header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(nutritionPlanService).deletePlan(planId);
	}

	@Test
	void shouldStartFromTheDefaultSchemeUnlessBlankIsAsked() {
		UUID memberId = UUID.randomUUID();
		when(nutritionPlanService.createForMember(eq(memberId), any(), isNull(), anyBoolean()))
				.thenReturn(plan("Ekim", false, true));
		String body = jsonMapper.writeValueAsString(
				new UpsertNutritionPlanRequest("Ekim", null, null, null, null, null, null));

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/nutrition-plans")
				.header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content(body))
				.hasStatus(HttpStatus.CREATED);
		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/nutrition-plans?blank=true")
				.header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content(body))
				.hasStatus(HttpStatus.CREATED);

		verify(nutritionPlanService).createForMember(eq(memberId), any(), isNull(), eq(false));
		verify(nutritionPlanService).createForMember(eq(memberId), any(), isNull(), eq(true));
	}

	@Test
	void shouldUpdateOneItemInPlace() {
		UUID itemId = UUID.randomUUID();
		when(nutritionPlanService.updateItem(eq(itemId), any())).thenReturn(plan("Ekim", false, true));

		assertThat(mvc.put().uri("/api/admin/meal-items/" + itemId)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new UpsertMealItemRequest("Pirinç", "150 g", null, null, null, null, null))))
				.hasStatus(HttpStatus.OK);
		verify(nutritionPlanService).updateItem(eq(itemId), any());
	}
}
