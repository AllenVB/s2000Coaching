package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Meal;
import com.s2000coaching.domain.MealItem;
import com.s2000coaching.domain.MealKind;
import com.s2000coaching.domain.NutritionDayType;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.NutritionPlan;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MealItemRepository;
import com.s2000coaching.repository.MealRepository;
import com.s2000coaching.repository.NutritionPlanRepository;
import com.s2000coaching.web.dto.UpsertMealItemRequest;
import com.s2000coaching.web.dto.UpsertMealRequest;
import com.s2000coaching.web.dto.UpsertNutritionPlanRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NutritionPlanServiceTest {

	@Mock
	private NutritionPlanRepository planRepository;

	@Mock
	private MealRepository mealRepository;

	@Mock
	private MealItemRepository mealItemRepository;

	@Mock
	private MemberService memberService;

	private NutritionPlanService nutritionPlanService;
	private Member member;

	@BeforeEach
	void setUp() {
		nutritionPlanService = new NutritionPlanService(planRepository, mealRepository, mealItemRepository,
				memberService);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
	}

	private NutritionPlan templateWithOneItem() {
		NutritionPlan template = new NutritionPlan(UUID.randomUUID(), null, "2400 kcal", null, false,
				Instant.now());
		template.setTargetCalories(2400);
		Meal meal = new Meal(UUID.randomUUID(), template, 1, "Kahvaltı", "08:00", null);
		template.getMeals().add(meal);
		meal.getItems().add(new MealItem(UUID.randomUUID(), meal, 1, "Yulaf", "80 g", 300, 10, 54, 6, null));
		return template;
	}

	@Nested
	class AssigningATemplate {

		@Test
		void shouldDeepCopyMealsAndItemsAndActivateTheCopy() {
			NutritionPlan template = templateWithOneItem();
			when(memberService.requireMember(member.getId())).thenReturn(member);
			when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(planRepository.findById(template.getId())).thenReturn(Optional.of(template));
			when(planRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())).thenReturn(List.of());

			var response = nutritionPlanService.createForMember(member.getId(),
					new UpsertNutritionPlanRequest("Ekim", null, 2500, 180, 250, 70, null), template.getId(),
					false);

			assertThat(response.active()).isTrue();
			assertThat(response.template()).isFalse();
			// Targets come from the request, not the template.
			assertThat(response.targetCalories()).isEqualTo(2500);
			assertThat(response.meals()).singleElement().satisfies(meal -> {
				assertThat(meal.name()).isEqualTo("Kahvaltı");
				assertThat(meal.id()).isNotEqualTo(template.getMeals().get(0).getId());
				assertThat(meal.items()).singleElement().satisfies(item -> {
					assertThat(item.name()).isEqualTo("Yulaf");
					assertThat(item.calories()).isEqualTo(300);
					assertThat(item.id()).isNotEqualTo(template.getMeals().get(0).getItems().get(0).getId());
				});
			});
		}

		@Test
		void shouldRefuseToCopyAPlanThatBelongsToSomeone() {
			NutritionPlan owned = new NutritionPlan(UUID.randomUUID(), member, "Birinin planı", null, true,
					Instant.now());
			when(memberService.requireMember(member.getId())).thenReturn(member);
			when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(planRepository.findById(owned.getId())).thenReturn(Optional.of(owned));

			assertThatThrownBy(() -> nutritionPlanService.createForMember(member.getId(),
					new UpsertNutritionPlanRequest("Kopya", null, null, null, null, null, null), owned.getId(),
					false))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Yalnızca şablonlar");
		}
	}

	@Nested
	class StartingFromTheDefaultScheme {

		private NutritionPlan create(boolean blank) {
			when(memberService.requireMember(member.getId())).thenReturn(member);
			when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(planRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())).thenReturn(List.of());
			var response = nutritionPlanService.createForMember(member.getId(),
					new UpsertNutritionPlanRequest("Ekim", null, null, null, null, null, new BigDecimal("4.0")),
					null, blank);
			assertThat(response.waterLiters()).isEqualByComparingTo("4.0");
			ArgumentCaptor<NutritionPlan> saved = ArgumentCaptor.forClass(NutritionPlan.class);
			verify(planRepository).save(saved.capture());
			return saved.getValue();
		}

		@Test
		void shouldListTheFoodsAndLeaveEveryAmountEmpty() {
			NutritionPlan plan = create(false);
			List<Meal> training = mealsOf(plan, NutritionDayType.TRAINING);

			assertThat(training).extracting(Meal::getName).containsExactly(
					"Sabah", "Öğlen", "Antrenman öncesi", "Antrenman sonrası", "Akşam", "Uyumadan önce",
					"Sabah kahvaltıdan sonra", "Antrenman sonrası", "Akşam öğünü ile beraber");
			assertThat(training.subList(0, 6)).allSatisfy(meal -> assertThat(meal.getKind())
					.isEqualTo(MealKind.MEAL));
			assertThat(training.subList(6, 9)).allSatisfy(meal -> assertThat(meal.getKind())
					.isEqualTo(MealKind.SUPPLEMENT));
			// The point of the scheme: foods are there, amounts are the coach's to write.
			assertThat(plan.getMeals().stream().flatMap(meal -> meal.getItems().stream()))
					.isNotEmpty()
					.allSatisfy(item -> {
						assertThat(item.getName()).isNotBlank();
						assertThat(item.getAmount()).isNull();
						assertThat(item.getCalories()).isNull();
					});
			assertThat(training.get(0).getItems()).extracting(MealItem::getDisplayOrder)
					.containsExactly(1, 2, 3, 4, 5, 6);
		}

		/** Every order number is used once across both halves, so adding a meal later cannot collide. */
		@Test
		void shouldAlsoLayOutARestDayWithoutTheBlocksAroundAWorkout() {
			NutritionPlan plan = create(false);

			assertThat(mealsOf(plan, NutritionDayType.REST)).extracting(Meal::getName).containsExactly(
					"Sabah", "Öğlen", "Akşam", "Uyumadan önce", "Sabah kahvaltıdan sonra",
					"Akşam öğünü ile beraber");
			assertThat(plan.getMeals()).extracting(Meal::getDisplayOrder).doesNotHaveDuplicates();
		}

		private static List<Meal> mealsOf(NutritionPlan plan, NutritionDayType dayType) {
			return plan.getMeals().stream().filter(meal -> meal.getDayType() == dayType).toList();
		}

		@Test
		void shouldStartEmptyWhenBlankIsAsked() {
			assertThat(create(true).getMeals()).isEmpty();
		}

		@Test
		void shouldCopyATemplateInsteadOfTheSchemeAndKeepSupplementsAsSupplements() {
			NutritionPlan template = templateWithOneItem();
			template.getMeals().get(0).setKind(MealKind.SUPPLEMENT);
			when(memberService.requireMember(member.getId())).thenReturn(member);
			when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(planRepository.findById(template.getId())).thenReturn(Optional.of(template));
			when(planRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())).thenReturn(List.of());

			var response = nutritionPlanService.createForMember(member.getId(),
					new UpsertNutritionPlanRequest("Ekim", null, null, null, null, null, null),
					template.getId(), false);

			assertThat(response.meals()).singleElement()
					.satisfies(meal -> assertThat(meal.kind()).isEqualTo(MealKind.SUPPLEMENT));
		}
	}

	@Test
	void shouldPutANewMealOnTheChosenKindOfDayAndDefaultToTraining() {
		NutritionPlan plan = new NutritionPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

		nutritionPlanService.addMeal(plan.getId(), new com.s2000coaching.web.dto.UpsertMealRequest("Ara", null, null,
				null, NutritionDayType.REST));
		nutritionPlanService.addMeal(plan.getId(), new com.s2000coaching.web.dto.UpsertMealRequest("Ara 2", null, null, null));

		assertThat(plan.getMeals()).extracting(Meal::getDayType)
				.containsExactly(NutritionDayType.REST, NutritionDayType.TRAINING);
	}

	@Test
	void shouldKeepTheDayTypeOfAMealWhenItIsEdited() {
		NutritionPlan plan = new NutritionPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		Meal meal = new Meal(UUID.randomUUID(), plan, 1, "Sabah", null, null);
		meal.setDayType(NutritionDayType.REST);
		plan.getMeals().add(meal);
		when(mealRepository.findById(meal.getId())).thenReturn(Optional.of(meal));

		nutritionPlanService.updateMeal(meal.getId(), new com.s2000coaching.web.dto.UpsertMealRequest("Kahvaltı", null,
				null, null, NutritionDayType.TRAINING));

		assertThat(meal.getName()).isEqualTo("Kahvaltı");
		assertThat(meal.getDayType()).isEqualTo(NutritionDayType.REST);
	}

	@Test
	void shouldStoreSeparateTargetsForRestDays() {
		NutritionPlan plan = new NutritionPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

		var response = nutritionPlanService.updatePlan(plan.getId(), new UpsertNutritionPlanRequest("Plan", null, 2800,
				180, 350, 80, null, 2200, 170, 220, 70));

		assertThat(response.targetCalories()).isEqualTo(2800);
		assertThat(response.restTargetCalories()).isEqualTo(2200);
		assertThat(response.restTargetProteinG()).isEqualTo(170);
		assertThat(response.restTargetCarbsG()).isEqualTo(220);
		assertThat(response.restTargetFatG()).isEqualTo(70);
	}

	@Test
	void shouldEditOneItemInPlaceAndKeepItsOrderAndMacros() {
		NutritionPlan plan = new NutritionPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		Meal meal = new Meal(UUID.randomUUID(), plan, 1, "Öğlen", null, null);
		plan.getMeals().add(meal);
		MealItem item = new MealItem(UUID.randomUUID(), meal, 3, "Pirinç", null, 200, 4, 44, 0, null);
		meal.getItems().add(item);
		when(mealItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

		nutritionPlanService.updateItem(item.getId(),
				new UpsertMealItemRequest(" Pirinç ", " 150 g ", 200, 4, 44, 0, "  "));

		assertThat(item.getAmount()).isEqualTo("150 g");
		assertThat(item.getName()).isEqualTo("Pirinç");
		assertThat(item.getNote()).isNull();
		assertThat(item.getDisplayOrder()).isEqualTo(3);
		assertThat(item.getCalories()).isEqualTo(200);
	}

	@Test
	void shouldRejectEditingAnItemThatDoesNotExist() {
		UUID missing = UUID.randomUUID();
		when(mealItemRepository.findById(missing)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> nutritionPlanService.updateItem(missing,
				new UpsertMealItemRequest("X", null, null, null, null, null, null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldLeaveOnlyOnePlanActive() {
		NutritionPlan older = new NutritionPlan(UUID.randomUUID(), member, "Eski", null, true, Instant.now());
		NutritionPlan newer = new NutritionPlan(UUID.randomUUID(), member, "Yeni", null, false, Instant.now());
		when(planRepository.findById(newer.getId())).thenReturn(Optional.of(newer));
		when(planRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId()))
				.thenReturn(List.of(newer, older));

		nutritionPlanService.activatePlan(newer.getId());

		assertThat(newer.isActive()).isTrue();
		assertThat(older.isActive()).isFalse();
	}

	@Test
	void shouldRefuseToActivateATemplate() {
		NutritionPlan template = templateWithOneItem();
		when(planRepository.findById(template.getId())).thenReturn(Optional.of(template));

		assertThatThrownBy(() -> nutritionPlanService.activatePlan(template.getId()))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldNumberMealsAndItemsInOrderAndTrimBlanks() {
		NutritionPlan plan = new NutritionPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

		nutritionPlanService.addMeal(plan.getId(), new UpsertMealRequest(" Kahvaltı ", "  ", null, null));
		nutritionPlanService.addMeal(plan.getId(), new UpsertMealRequest("Öğle", "12:30", null, null));

		assertThat(plan.getMeals()).extracting(Meal::getDisplayOrder).containsExactly(1, 2);
		assertThat(plan.getMeals().get(0).getName()).isEqualTo("Kahvaltı");
		assertThat(plan.getMeals().get(0).getTimeHint()).isNull();

		Meal first = plan.getMeals().get(0);
		when(mealRepository.findById(first.getId())).thenReturn(Optional.of(first));

		nutritionPlanService.addItem(first.getId(),
				new UpsertMealItemRequest(" Yulaf ", "80 g", 300, 10, 54, 6, null));

		assertThat(first.getItems()).singleElement().satisfies(item -> {
			assertThat(item.getDisplayOrder()).isEqualTo(1);
			assertThat(item.getName()).isEqualTo("Yulaf");
		});
	}

	@Test
	void shouldReportNoPlanWhenNoneIsActive() {
		when(planRepository.findByMemberIdAndActiveTrue(member.getId())).thenReturn(Optional.empty());

		assertThat(nutritionPlanService.findActivePlan(member.getId())).isEmpty();
	}
}
