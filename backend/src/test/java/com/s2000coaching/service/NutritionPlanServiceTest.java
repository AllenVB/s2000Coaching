package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Meal;
import com.s2000coaching.domain.MealItem;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.NutritionPlan;
import com.s2000coaching.exception.BadRequestException;
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
					new UpsertNutritionPlanRequest("Ekim", null, 2500, 180, 250, 70), template.getId());

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
					new UpsertNutritionPlanRequest("Kopya", null, null, null, null, null), owned.getId()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Yalnızca şablonlar");
		}
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

		nutritionPlanService.addMeal(plan.getId(), new UpsertMealRequest(" Kahvaltı ", "  ", null));
		nutritionPlanService.addMeal(plan.getId(), new UpsertMealRequest("Öğle", "12:30", null));

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
