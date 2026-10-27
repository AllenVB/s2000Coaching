package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.NutritionPlanService;
import com.s2000coaching.service.WorkoutLogService;
import com.s2000coaching.web.dto.NutritionPlanResponse;
import com.s2000coaching.web.dto.SaveWorkoutLogRequest;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.WorkoutLogResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(MemberTrainingController.class)
class MemberTrainingControllerTest {

	private static final String BEARER = "Bearer token-123";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private WorkoutLogService workoutLogService;

	@MockitoBean
	private NutritionPlanService nutritionPlanService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	private static Member member() {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		member.activate(Instant.now());
		return member;
	}

	@Test
	void shouldAnswer204WhenNoPlanIsAssignedYet() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(workoutLogService.findActivePlan(member.getId())).thenReturn(Optional.empty());

		assertThat(mvc.get().uri("/api/member/training-plan").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void shouldReturnTheActivePlan() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(workoutLogService.findActivePlan(member.getId())).thenReturn(Optional.of(
				new TrainingPlanResponse(UUID.randomUUID(), "Ekim Bloğu", null, false, true, Instant.now(),
						List.of())));

		assertThat(mvc.get().uri("/api/member/training-plan").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.name").isEqualTo("Ekim Bloğu");
	}

	@Test
	void shouldAnswer204WhenNoNutritionPlanIsAssignedYet() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(nutritionPlanService.findActivePlan(member.getId())).thenReturn(Optional.empty());

		assertThat(mvc.get().uri("/api/member/nutrition-plan").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void shouldReturnTheActiveNutritionPlan() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(nutritionPlanService.findActivePlan(member.getId())).thenReturn(Optional.of(
				new NutritionPlanResponse(UUID.randomUUID(), "Ekim Beslenmesi", null, false, true, 2400, 180,
						250, 70, null, Instant.now(), List.of())));

		assertThat(mvc.get().uri("/api/member/nutrition-plan").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.targetCalories").isEqualTo(2400);
	}

	@Test
	void shouldReturn401WithoutASession() {
		when(memberAuthService.requireMember(null)).thenThrow(new UnauthorizedException("Oturum bulunamadı."));

		assertThat(mvc.get().uri("/api/member/training-plan")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(workoutLogService, never()).findActivePlan(any());
	}

	@Test
	void shouldSaveASession() {
		Member member = member();
		UUID dayId = UUID.randomUUID();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(workoutLogService.save(eq(member), any())).thenReturn(new WorkoutLogResponse(
				UUID.randomUUID(), dayId, "Gün 1", LocalDate.now(), null, List.of()));

		var request = new SaveWorkoutLogRequest(dayId, LocalDate.now(), null, List.of(
				new SaveWorkoutLogRequest.SetEntry(UUID.randomUUID(), 1, 10, new BigDecimal("60.0"), null)));

		assertThat(mvc.post().uri("/api/member/workout-logs")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.dayName").isEqualTo("Gün 1");
	}

	@Test
	void shouldReturn400ForAFutureSession() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);

		var request = new SaveWorkoutLogRequest(UUID.randomUUID(), LocalDate.now().plusDays(1), null,
				List.of());

		assertThat(mvc.post().uri("/api/member/workout-logs")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(workoutLogService, never()).save(any(), any());
	}
}
