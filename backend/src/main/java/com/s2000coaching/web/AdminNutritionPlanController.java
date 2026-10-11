package com.s2000coaching.web;

import com.s2000coaching.service.NutritionPlanService;
import com.s2000coaching.web.dto.NutritionPlanResponse;
import com.s2000coaching.web.dto.UpsertMealItemRequest;
import com.s2000coaching.web.dto.UpsertMealRequest;
import com.s2000coaching.web.dto.UpsertNutritionPlanRequest;
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

@RestController
public class AdminNutritionPlanController {

	private final NutritionPlanService nutritionPlanService;

	public AdminNutritionPlanController(NutritionPlanService nutritionPlanService) {
		this.nutritionPlanService = nutritionPlanService;
	}

	@GetMapping("/api/admin/nutrition-plan-templates")
	public List<NutritionPlanResponse> listTemplates() {
		return nutritionPlanService.listTemplates();
	}

	@PostMapping("/api/admin/nutrition-plan-templates")
	public ResponseEntity<NutritionPlanResponse> createTemplate(
			@Valid @RequestBody UpsertNutritionPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(nutritionPlanService.createTemplate(request));
	}

	@GetMapping("/api/admin/members/{memberId}/nutrition-plans")
	public List<NutritionPlanResponse> listForMember(@PathVariable UUID memberId) {
		return nutritionPlanService.listForMember(memberId);
	}

	/** {@code templateId} copies that template's meals and items into the new plan. */
	@PostMapping("/api/admin/members/{memberId}/nutrition-plans")
	public ResponseEntity<NutritionPlanResponse> createForMember(
			@PathVariable UUID memberId,
			@RequestParam(required = false) UUID templateId,
			@Valid @RequestBody UpsertNutritionPlanRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(nutritionPlanService.createForMember(memberId, request, templateId));
	}

	@PutMapping("/api/admin/nutrition-plans/{planId}")
	public NutritionPlanResponse updatePlan(@PathVariable UUID planId,
			@Valid @RequestBody UpsertNutritionPlanRequest request) {
		return nutritionPlanService.updatePlan(planId, request);
	}

	@PostMapping("/api/admin/nutrition-plans/{planId}/activation")
	public NutritionPlanResponse activatePlan(@PathVariable UUID planId) {
		return nutritionPlanService.activatePlan(planId);
	}

	@DeleteMapping("/api/admin/nutrition-plans/{planId}")
	public ResponseEntity<Void> deletePlan(@PathVariable UUID planId) {
		nutritionPlanService.deletePlan(planId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/admin/nutrition-plans/{planId}/meals")
	public NutritionPlanResponse addMeal(@PathVariable UUID planId,
			@Valid @RequestBody UpsertMealRequest request) {
		return nutritionPlanService.addMeal(planId, request);
	}

	@PutMapping("/api/admin/meals/{mealId}")
	public NutritionPlanResponse updateMeal(@PathVariable UUID mealId,
			@Valid @RequestBody UpsertMealRequest request) {
		return nutritionPlanService.updateMeal(mealId, request);
	}

	@DeleteMapping("/api/admin/meals/{mealId}")
	public NutritionPlanResponse deleteMeal(@PathVariable UUID mealId) {
		return nutritionPlanService.deleteMeal(mealId);
	}

	@PostMapping("/api/admin/meals/{mealId}/items")
	public NutritionPlanResponse addItem(@PathVariable UUID mealId,
			@Valid @RequestBody UpsertMealItemRequest request) {
		return nutritionPlanService.addItem(mealId, request);
	}

	@DeleteMapping("/api/admin/meal-items/{itemId}")
	public NutritionPlanResponse deleteItem(@PathVariable UUID itemId) {
		return nutritionPlanService.deleteItem(itemId);
	}
}
