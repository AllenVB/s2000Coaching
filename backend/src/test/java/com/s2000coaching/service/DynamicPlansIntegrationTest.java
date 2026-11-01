package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MealKind;
import com.s2000coaching.domain.NutritionDayType;
import com.s2000coaching.repository.ExerciseRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.web.dto.UpsertMealItemRequest;
import com.s2000coaching.web.dto.UpsertMealRequest;
import com.s2000coaching.web.dto.UpsertNutritionPlanRequest;
import com.s2000coaching.web.dto.UpsertPlanDayRequest;
import com.s2000coaching.web.dto.UpsertPlanExerciseRequest;
import com.s2000coaching.web.dto.UpsertTrainingPlanRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Against the real schema: ten sets of rep targets must fit their column, an exercise typed by name
 * must be storable without a muscle group, and the two halves of a nutrition plan must survive a round
 * trip with their own targets.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DynamicPlansIntegrationTest {

	@Autowired
	private TrainingPlanService trainingPlanService;
	@Autowired
	private NutritionPlanService nutritionPlanService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private ExerciseRepository exerciseRepository;

	private Member newMember() {
		Member member = new Member(UUID.randomUUID(), UUID.randomUUID() + "@example.com", "Test Üye", "", 180,
				new BigDecimal("80.00"), "yok", MemberStatus.ACTIVE, Instant.now());
		member.assignMemberNumber(ThreadLocalRandom.current().nextInt(50_000, 99_999));
		return memberRepository.save(member);
	}

	@Test
	void shouldStoreAPlanWithSevenDaysAndTenSetsOfTypedExercises() {
		Member member = newMember();
		var plan = trainingPlanService.createForMember(member.getId(), new UpsertTrainingPlanRequest("Plan", null), null,
				false);
		assertThat(plan.days()).extracting(day -> day.name()).containsExactly("Gün 1");

		for (int number = 2; number <= 7; number++) {
			plan = trainingPlanService.addDay(plan.id(), new UpsertPlanDayRequest("Gün " + number, null));
		}
		assertThat(plan.days()).hasSize(7);

		String name = "Typed Press " + UUID.randomUUID();
		var withExercise = trainingPlanService.addExercise(plan.days().get(0).id(), new UpsertPlanExerciseRequest(null,
				name, null, null, List.of("16", "16", "14", "14", "12", "12", "10", "10", "8", "8"), null, null, null));

		var saved = withExercise.days().get(0).exercises().get(0);
		assertThat(saved.setCount()).isEqualTo(10);
		assertThat(saved.setReps()).containsExactly("16", "16", "14", "14", "12", "12", "10", "10", "8", "8");
		assertThat(saved.exercise().muscleGroup()).isNull();
		assertThat(exerciseRepository.findFirstByNameIgnoreCase(name.toUpperCase())).isPresent();
	}

	@Test
	void shouldKeepTrainingAndRestHalvesOfANutritionPlanApart() {
		Member member = newMember();
		var plan = nutritionPlanService.createForMember(member.getId(), new UpsertNutritionPlanRequest("Plan", null, 2800,
				180, 350, 80, null, 2200, 170, 220, 70), null, false);

		assertThat(plan.restTargetCalories()).isEqualTo(2200);
		assertThat(plan.meals()).anySatisfy(meal -> assertThat(meal.dayType()).isEqualTo(NutritionDayType.TRAINING));
		assertThat(plan.meals()).anySatisfy(meal -> assertThat(meal.dayType()).isEqualTo(NutritionDayType.REST));

		var withMeal = nutritionPlanService.addMeal(plan.id(),
				new UpsertMealRequest("Dinlenme ara", null, null, MealKind.MEAL, NutritionDayType.REST));
		var added = withMeal.meals().get(withMeal.meals().size() - 1);
		assertThat(added.dayType()).isEqualTo(NutritionDayType.REST);

		var withItem = nutritionPlanService.addItem(added.id(), new UpsertMealItemRequest("Yoğurt", "200 g", null, null,
				null, null, null));
		assertThat(withItem.meals().get(withItem.meals().size() - 1).items()).hasSize(1);
	}

	@Test
	void shouldGiveEachMemberAnIndependentActiveCopyOfAPlan() {
		Member source = newMember();
		Member first = newMember();
		Member second = newMember();
		var plan = nutritionPlanService.createForMember(source.getId(), new UpsertNutritionPlanRequest("Kesim", null, 2800,
				180, 350, 80, null, 2200, 170, 220, 70), null, false);
		var training = trainingPlanService.createForMember(source.getId(), new UpsertTrainingPlanRequest("Itme", null),
				null, false);

		int assigned = nutritionPlanService.assignCopies(plan.id(), List.of(source.getId(), first.getId(), second.getId()));
		int trainingAssigned = trainingPlanService.assignCopies(training.id(), List.of(first.getId(), second.getId()));

		// The source member is skipped: copying a plan onto its own owner would only duplicate it.
		assertThat(assigned).isEqualTo(2);
		assertThat(trainingAssigned).isEqualTo(2);
		var firstPlans = nutritionPlanService.listForMember(first.getId());
		assertThat(firstPlans).hasSize(1);
		assertThat(firstPlans.get(0).targetCalories()).isEqualTo(2800);
		assertThat(firstPlans.get(0).restTargetCalories()).isEqualTo(2200);
		assertThat(firstPlans.get(0).active()).isTrue();
		assertThat(firstPlans.get(0).id()).isNotEqualTo(plan.id());
		assertThat(trainingPlanService.listForMember(second.getId())).hasSize(1);
	}
}
