package com.s2000coaching.web;

import com.s2000coaching.service.TrainingPlanService;
import com.s2000coaching.service.WorkoutLogService;
import com.s2000coaching.web.dto.TrainingPlanResponse;
import com.s2000coaching.web.dto.UpsertPlanDayRequest;
import com.s2000coaching.web.dto.UpsertPlanExerciseRequest;
import com.s2000coaching.web.dto.UpsertTrainingPlanRequest;
import com.s2000coaching.web.dto.WorkoutLogResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Plan authoring. Templates live at the same endpoints with no member attached. */
@RestController
public class AdminTrainingPlanController {

	private final TrainingPlanService trainingPlanService;
	private final WorkoutLogService workoutLogService;

	public AdminTrainingPlanController(TrainingPlanService trainingPlanService,
			WorkoutLogService workoutLogService) {
		this.trainingPlanService = trainingPlanService;
		this.workoutLogService = workoutLogService;
	}

	@GetMapping("/api/admin/training-plan-templates")
	public List<TrainingPlanResponse> listTemplates() {
		return trainingPlanService.listTemplates();
	}

	/** {@code blank=true} skips the standard days "Gün 1" to "Gün 5" and creates an empty template. */
	@PostMapping("/api/admin/training-plan-templates")
	public ResponseEntity<TrainingPlanResponse> createTemplate(
			@RequestParam(defaultValue = "false") boolean blank,
			@Valid @RequestBody UpsertTrainingPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(trainingPlanService.createTemplate(request, blank));
	}

	@GetMapping("/api/admin/members/{memberId}/training-plans")
	public List<TrainingPlanResponse> listForMember(@PathVariable UUID memberId) {
		return trainingPlanService.listForMember(memberId);
	}

	/**
	 * {@code templateId} copies that template's days and exercises into the new plan. Without
	 * one the plan starts with empty "Gün 1" to "Gün 5"; {@code blank=true} starts with no days.
	 */
	@PostMapping("/api/admin/members/{memberId}/training-plans")
	public ResponseEntity<TrainingPlanResponse> createForMember(
			@PathVariable UUID memberId,
			@RequestParam(required = false) UUID templateId,
			@RequestParam(defaultValue = "false") boolean blank,
			@Valid @RequestBody UpsertTrainingPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(trainingPlanService.createForMember(memberId, request, templateId, blank));
	}

	@GetMapping("/api/admin/members/{memberId}/workout-logs")
	public List<WorkoutLogResponse> listWorkoutLogs(@PathVariable UUID memberId) {
		return workoutLogService.listLogsAsCoach(memberId);
	}

	@PutMapping("/api/admin/training-plans/{planId}")
	public TrainingPlanResponse updatePlan(@PathVariable UUID planId,
			@Valid @RequestBody UpsertTrainingPlanRequest request) {
		return trainingPlanService.updatePlan(planId, request);
	}

	@PostMapping("/api/admin/training-plans/{planId}/activation")
	public TrainingPlanResponse activatePlan(@PathVariable UUID planId) {
		return trainingPlanService.activatePlan(planId);
	}

	@DeleteMapping("/api/admin/training-plans/{planId}")
	public ResponseEntity<Void> deletePlan(@PathVariable UUID planId) {
		trainingPlanService.deletePlan(planId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/admin/training-plans/{planId}/days")
	public TrainingPlanResponse addDay(@PathVariable UUID planId,
			@Valid @RequestBody UpsertPlanDayRequest request) {
		return trainingPlanService.addDay(planId, request);
	}

	@PutMapping("/api/admin/training-plan-days/{dayId}")
	public TrainingPlanResponse updateDay(@PathVariable UUID dayId,
			@Valid @RequestBody UpsertPlanDayRequest request) {
		return trainingPlanService.updateDay(dayId, request);
	}

	@DeleteMapping("/api/admin/training-plan-days/{dayId}")
	public TrainingPlanResponse deleteDay(@PathVariable UUID dayId) {
		return trainingPlanService.deleteDay(dayId);
	}

	@PostMapping("/api/admin/training-plan-days/{dayId}/exercises")
	public TrainingPlanResponse addExercise(@PathVariable UUID dayId,
			@Valid @RequestBody UpsertPlanExerciseRequest request) {
		return trainingPlanService.addExercise(dayId, request);
	}

	@PutMapping("/api/admin/training-plan-exercises/{planExerciseId}")
	public TrainingPlanResponse updateExercise(@PathVariable UUID planExerciseId,
			@Valid @RequestBody UpsertPlanExerciseRequest request) {
		return trainingPlanService.updateExercise(planExerciseId, request);
	}

	@DeleteMapping("/api/admin/training-plan-exercises/{planExerciseId}")
	public TrainingPlanResponse deleteExercise(@PathVariable UUID planExerciseId) {
		return trainingPlanService.deleteExercise(planExerciseId);
	}
}
