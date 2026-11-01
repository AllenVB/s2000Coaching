package com.s2000coaching.service;

import com.s2000coaching.domain.Meal;
import com.s2000coaching.domain.MealItem;
import com.s2000coaching.domain.MealKind;
import com.s2000coaching.domain.NutritionDayType;
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

	/**
	 * A new template starts from {@link DefaultNutritionScheme} unless {@code blank} is set, so
	 * the coach never begins from an empty page; {@code blank} is for the rare plan that does not
	 * look like the usual ones.
	 */
	@Transactional
	public NutritionPlanResponse createTemplate(UpsertNutritionPlanRequest request, boolean blank) {
		NutritionPlan plan = planRepository.save(new NutritionPlan(UUID.randomUUID(), null,
				request.name().trim(), blankToNull(request.note()), false, Instant.now()));
		applyTargets(plan, request);
		if (!blank) {
			DefaultNutritionScheme.seed(plan);
		}
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

	/**
	 * Where the plan's meals come from, in order: the chosen template; otherwise the default
	 * scheme (foods listed, amounts empty); otherwise nothing, when {@code blank} is set.
	 */
	@Transactional
	public NutritionPlanResponse createForMember(UUID memberId, UpsertNutritionPlanRequest request,
			UUID templateId, boolean blank) {
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
		} else if (!blank) {
			DefaultNutritionScheme.seed(plan);
		}

		deactivateOtherPlans(memberId, plan.getId());
		return NutritionPlanResponse.from(plan);
	}

	/** Same as the training counterpart: an independent active copy (meals and targets) for each member. */
	@Transactional
	public int assignCopies(UUID sourcePlanId, List<UUID> memberIds) {
		NutritionPlan source = getPlanOrThrow(sourcePlanId);
		List<Member> members = memberIds.stream().distinct().map(memberService::requireMember).toList();
		Instant now = Instant.now();
		int assigned = 0;
		for (Member member : members) {
			if (source.getMember() != null && source.getMember().getId().equals(member.getId())) {
				continue;
			}
			NutritionPlan plan = planRepository.save(new NutritionPlan(UUID.randomUUID(), member, source.getName(),
					source.getNote(), true, now));
			plan.setTargetCalories(source.getTargetCalories());
			plan.setTargetProteinG(source.getTargetProteinG());
			plan.setTargetCarbsG(source.getTargetCarbsG());
			plan.setTargetFatG(source.getTargetFatG());
			plan.setRestTargetCalories(source.getRestTargetCalories());
			plan.setRestTargetProteinG(source.getRestTargetProteinG());
			plan.setRestTargetCarbsG(source.getRestTargetCarbsG());
			plan.setRestTargetFatG(source.getRestTargetFatG());
			plan.setWaterLiters(source.getWaterLiters());
			plan.setUpdatedAt(now);
			copyInto(plan, source);
			deactivateOtherPlans(member.getId(), plan.getId());
			assigned++;
		}
		return assigned;
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
		Meal meal = new Meal(UUID.randomUUID(), plan, nextOrder, request.name().trim(),
				blankToNull(request.timeHint()), blankToNull(request.note()));
		meal.setKind(kindOrMeal(request.kind()));
		meal.setDayType(request.dayType() == null ? NutritionDayType.TRAINING : request.dayType());
		plan.getMeals().add(meal);
		plan.setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(plan);
	}

	@Transactional
	public NutritionPlanResponse updateMeal(UUID mealId, UpsertMealRequest request) {
		Meal meal = getMealOrThrow(mealId);
		meal.setName(request.name().trim());
		meal.setTimeHint(blankToNull(request.timeHint()));
		meal.setNote(blankToNull(request.note()));
		if (request.kind() != null) {
			meal.setKind(request.kind());
		}
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

	/** Replaces every field of one item; the table view edits a cell and sends the row back. */
	@Transactional
	public NutritionPlanResponse updateItem(UUID itemId, UpsertMealItemRequest request) {
		MealItem item = getItemOrThrow(itemId);
		item.setName(request.name().trim());
		item.setAmount(blankToNull(request.amount()));
		item.setCalories(request.calories());
		item.setProteinG(request.proteinG());
		item.setCarbsG(request.carbsG());
		item.setFatG(request.fatG());
		item.setNote(blankToNull(request.note()));
		item.getMeal().getPlan().setUpdatedAt(Instant.now());
		return NutritionPlanResponse.from(item.getMeal().getPlan());
	}

	@Transactional
	public NutritionPlanResponse deleteItem(UUID itemId) {
		MealItem item = getItemOrThrow(itemId);
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
			meal.setKind(sourceMeal.getKind());
			meal.setDayType(sourceMeal.getDayType());
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
		plan.setRestTargetCalories(request.restTargetCalories());
		plan.setRestTargetProteinG(request.restTargetProteinG());
		plan.setRestTargetCarbsG(request.restTargetCarbsG());
		plan.setRestTargetFatG(request.restTargetFatG());
		plan.setWaterLiters(request.waterLiters());
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

	private MealItem getItemOrThrow(UUID itemId) {
		return mealItemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Öğün kalemi bulunamadı: " + itemId));
	}

	private static MealKind kindOrMeal(MealKind kind) {
		return kind == null ? MealKind.MEAL : kind;
	}

	private Meal getMealOrThrow(UUID mealId) {
		return mealRepository.findById(mealId)
				.orElseThrow(() -> new NotFoundException("Öğün bulunamadı: " + mealId));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
