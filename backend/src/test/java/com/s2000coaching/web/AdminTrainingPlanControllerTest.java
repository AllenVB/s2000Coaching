package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.TrainingPlanService;
import com.s2000coaching.service.WorkoutLogService;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.UpsertTrainingPlanRequest;
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

@WebMvcTest(AdminTrainingPlanController.class)
class AdminTrainingPlanControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private TrainingPlanService trainingPlanService;

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

	@MockitoBean
	private WorkoutLogService workoutLogService;

	private static TrainingPlanResponse plan(String name, boolean template, boolean active) {
		return new TrainingPlanResponse(UUID.randomUUID(), name, null, template, active, Instant.now(),
				List.of());
	}

	@Test
	void shouldRejectTemplatesWithoutTheAdminToken() {
		assertThat(mvc.get().uri("/api/admin/training-plan-templates")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(trainingPlanService, never()).listTemplates();
	}

	@Test
	void shouldCreateATemplate() {
		when(trainingPlanService.createTemplate(any())).thenReturn(plan("PPL", true, false));

		assertThat(mvc.post().uri("/api/admin/training-plan-templates")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertTrainingPlanRequest("PPL", null))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.template").isEqualTo(true);
	}

	@Test
	void shouldReturn400ForANamelessPlan() {
		assertThat(mvc.post().uri("/api/admin/training-plan-templates")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertTrainingPlanRequest(" ", null))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(trainingPlanService, never()).createTemplate(any());
	}

	@Test
	void shouldPassTheTemplateIdThroughWhenAssigning() {
		UUID memberId = UUID.randomUUID();
		UUID templateId = UUID.randomUUID();
		when(trainingPlanService.createForMember(eq(memberId), any(), eq(templateId)))
				.thenReturn(plan("Ekim Bloğu", false, true));

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/training-plans?templateId=" + templateId)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertTrainingPlanRequest("Ekim Bloğu", null))))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.active").isEqualTo(true);
	}

	@Test
	void shouldAssignAnEmptyPlanWhenNoTemplateIsGiven() {
		UUID memberId = UUID.randomUUID();
		when(trainingPlanService.createForMember(eq(memberId), any(), isNull()))
				.thenReturn(plan("Boş", false, true));

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/training-plans")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new UpsertTrainingPlanRequest("Boş", null))))
				.hasStatus(HttpStatus.CREATED);
		verify(trainingPlanService).createForMember(eq(memberId), any(), isNull());
	}

	@Test
	void shouldReturn204WhenDeletingAPlan() {
		UUID planId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/training-plans/" + planId).header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(trainingPlanService).deletePlan(planId);
	}
}
