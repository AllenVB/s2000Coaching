package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.NutritionPlanService;
import com.s2000coaching.service.WorkoutLogService;
import com.s2000coaching.web.dto.NutritionPlanResponse;
import com.s2000coaching.web.dto.SaveWorkoutLogRequest;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.WorkoutLogResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The member's own programme: read the active plans, record sessions. */
@RestController
public class MemberTrainingController {

	private final WorkoutLogService workoutLogService;
	private final NutritionPlanService nutritionPlanService;
	private final MemberAuthService memberAuthService;

	public MemberTrainingController(WorkoutLogService workoutLogService,
			NutritionPlanService nutritionPlanService,
			MemberAuthService memberAuthService) {
		this.workoutLogService = workoutLogService;
		this.nutritionPlanService = nutritionPlanService;
		this.memberAuthService = memberAuthService;
	}

	/** 204 when the coach has not assigned a nutrition plan yet. */
	@GetMapping("/api/member/nutrition-plan")
	public ResponseEntity<NutritionPlanResponse> activeNutritionPlan(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return nutritionPlanService.findActivePlan(member.getId())
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	/** 204 when the coach has not assigned a plan yet — an empty plan is not an error. */
	@GetMapping("/api/member/training-plan")
	public ResponseEntity<TrainingPlanResponse> activePlan(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return workoutLogService.findActivePlan(member.getId())
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/api/member/workout-logs")
	public List<WorkoutLogResponse> listLogs(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return workoutLogService.listLogs(member.getId());
	}

	@PostMapping("/api/member/workout-logs")
	public WorkoutLogResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody SaveWorkoutLogRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return workoutLogService.save(member, request);
	}

	@DeleteMapping("/api/member/workout-logs/{workoutLogId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID workoutLogId) {
		Member member = memberAuthService.requireMember(authorization);
		workoutLogService.deleteOwn(member, workoutLogId);
		return ResponseEntity.noContent().build();
	}
}
