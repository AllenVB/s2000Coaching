package com.s2000coaching.service;

import com.s2000coaching.domain.Meal;
import com.s2000coaching.domain.MealItem;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.NutritionPlan;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MealItemRepository;
import com.s2000coaching.repository.MealRepository;
import com.s2000coaching.repository.NutritionPlanRepository;
import com.s2000coaching.web.dto.NutritionPlanResponse;
import com.s2000coaching.web.dto.UpsertMealItemRequest;
import com.s2000coaching.web.dto.UpsertMealRequest;
import com.s2000coaching.web.dto.UpsertNutritionPlanRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nutrition plan authoring, deliberately the same shape as {@link TrainingPlanService}:
 * templates copy onto a member, and exactly one plan per member is active.
 */
@Service
public class NutritionPlanService {

	private final NutritionPlanRepository planRepository;
	private final MealRepository mealRepository;
	private final MealItemRepository mealItemRepository;
	private final MemberService memberService;

	public NutritionPlanService(NutritionPlanRepository planRepository,
			MealRepository mealRepository,
			MealItemRepository mealItemRepository,
			MemberService memberService) {
		this.planRepository = planRepository;
		this.mealRepository = mealRepository;
		this.mealItemRepository = mealItemRepository;
		this.memberService = memberService;
	}

	@Transactional(readOnly = true)
	public List<NutritionPlanResponse> listTemplates() {
		return planRepository.findAllByMemberIsNullOrderByNameAsc().stream()
				.map(NutritionPlanResponse::from)
				.toList();
	}

	@Transactional
	public NutritionPlanResponse createTemplate(UpsertNutritionPlanRequest request) {
		NutritionPlan plan = planRepository.save(new NutritionPlan(UUID.randomUUID(), null,
				request.name().trim(), blankToNull(request.note()), false, Instant.now()));
		applyTargets(plan, request);
		return NutritionPlanResponse.from(plan);
	}

	@Transactional(readOnly = true)
	public List<NutritionPlanResponse> listForMember(UUID memberId) {
		memberService.requireMember(memberId);
		return planRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.map(NutritionPlanResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<NutritionPlanResponse> findActivePlan(UUID memberId) {
		return planRepository.findByMemberIdAndActiveTrue(memberId).map(NutritionPlanResponse::from);
	}

	@Transactional
	public NutritionPlanResponse createForMember(UUID memberId, UpsertNutritionPlanRequest request,
			UUID templateId) {
		Member member = memberService.requireMember(memberId);
		// Assigned UUIDs mean save() merges and returns a different managed instance; keep
		// that one, or the meals added below would be written to a detached copy.
		NutritionPlan plan = planRepository.save(new NutritionPlan(UUID.randomUUID(), member,
				request.name().trim(), blankToNull(request.note()), true, Instant.now()));
		applyTargets(plan, request);

		if (templateId != null) {
			NutritionPlan template = getPlanOrThrow(templateId);
			if (!template.isTemplate()) {
				throw new BadRequestException("Yalnızca şablonlar kopyalanabilir.");
			}
			copyInto(plan, template);
		}

		deactivateOtherPlans(memberId, plan.getId());
		return NutritionPlanResponse.from(plan);
	}

	@Transactional
	public NutritionPlanResponse updatePlan(UUID planId, UpsertNutritionPlanRequest request) {
		NutritionPlan plan = getPlanOrThrow(planId);
		plan.setName(request.name().trim());
		plan.setNote(blankToNull(request.note()));
		applyTargets(plan, request);
		return NutritionPlanResponse.from(plan);
	}

	@Transactional
	public NutritionPlanResponse activatePlan(UUID planId) {
		NutritionPlan plan = getPlanOrThrow(planId);
		if (plan.isTemplate()) {
			throw new BadRequestException("Şablonlar aktifleştirilemez; önce bir üyeye atayın.");
		}
		plan.setActive(true);
		plan.setUpdatedAt(Instant.now());
		deactivateOtherPlans(plan.getMember().getId(), plan.getId());
		return NutritionPlanResponse.from(plan);
	}

	@Transactional
	public void deletePlan(UUID planId) {
		planRepository.delete(getPlanOrThrow(planId));
	}

	@Transactional
	public NutritionPlanResponse addMeal(UUID planId, UpsertMealRequest request) {
		NutritionPlan plan = getPlanOrThrow(planId);
		int nextOrder = plan.getMeals().stream().mapToInt(Meal::getDisplayOrder).max().orElse(0) + 1;
		// Cascaded from the managed plan; an explicit save() would merge a duplicate.
		plan.getMeals().add(new Meal(UUID.randomUUID(), plan, nextOrder, request.name().trim(),
				blankToNull(request.timeHint()), blankToNull(request.note())));
		plan.setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(plan);
	}

	@Transactional
	public NutritionPlanResponse updateMeal(UUID mealId, UpsertMealRequest request) {
		Meal meal = getMealOrThrow(mealId);
		meal.setName(request.name().trim());
		meal.setTimeHint(blankToNull(request.timeHint()));
		meal.setNote(blankToNull(request.note()));
		meal.getPlan().setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(meal.getPlan());
	}

	@Transactional
	public NutritionPlanResponse deleteMeal(UUID mealId) {
		Meal meal = getMealOrThrow(mealId);
		NutritionPlan plan = meal.getPlan();
		plan.getMeals().remove(meal);
		plan.setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(plan);
	}

	@Transactional
	public NutritionPlanResponse addItem(UUID mealId, UpsertMealItemRequest request) {
		Meal meal = getMealOrThrow(mealId);
		int nextOrder = meal.getItems().stream().mapToInt(MealItem::getDisplayOrder).max().orElse(0) + 1;
		meal.getItems().add(new MealItem(UUID.randomUUID(), meal, nextOrder, request.name().trim(),
				blankToNull(request.amount()), request.calories(), request.proteinG(), request.carbsG(),
				request.fatG(), blankToNull(request.note())));
		meal.getPlan().setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(meal.getPlan());
	}

	@Transactional
	public NutritionPlanResponse deleteItem(UUID itemId) {
		MealItem item = mealItemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Öğün kalemi bulunamadı: " + itemId));
		Meal meal = item.getMeal();
		meal.getItems().remove(item);
		meal.getPlan().setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(meal.getPlan());
	}

	/** Deep copy: new rows all the way down, so the two plans share nothing afterwards. */
	private void copyInto(NutritionPlan target, NutritionPlan source) {
		for (Meal sourceMeal : source.getMeals()) {
			Meal meal = new Meal(UUID.randomUUID(), target, sourceMeal.getDisplayOrder(),
					sourceMeal.getName(), sourceMeal.getTimeHint(), sourceMeal.getNote());
			target.getMeals().add(meal);

			for (MealItem sourceItem : sourceMeal.getItems()) {
				meal.getItems().add(new MealItem(UUID.randomUUID(), meal, sourceItem.getDisplayOrder(),
						sourceItem.getName(), sourceItem.getAmount(), sourceItem.getCalories(),
						sourceItem.getProteinG(), sourceItem.getCarbsG(), sourceItem.getFatG(),
						sourceItem.getNote()));
			}
		}
	}

	private void applyTargets(NutritionPlan plan, UpsertNutritionPlanRequest request) {
		plan.setTargetCalories(request.targetCalories());
		plan.setTargetProteinG(request.targetProteinG());
		plan.setTargetCarbsG(request.targetCarbsG());
		plan.setTargetFatG(request.targetFatG());
		plan.setUpdatedAt(Instant.now());
	}

	private void deactivateOtherPlans(UUID memberId, UUID keepActiveId) {
		planRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.filter(other -> !other.getId().equals(keepActiveId))
				.forEach(other -> other.setActive(false));
	}

	private NutritionPlan getPlanOrThrow(UUID planId) {
		return planRepository.findById(planId)
				.orElseThrow(() -> new NotFoundException("Beslenme planı bulunamadı: " + planId));
	}

	private Meal getMealOrThrow(UUID mealId) {
		return mealRepository.findById(mealId)
				.orElseThrow(() -> new NotFoundException("Öğün bulunamadı: " + mealId));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
