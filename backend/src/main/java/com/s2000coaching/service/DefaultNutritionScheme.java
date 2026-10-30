package com.s2000coaching.service;

import com.s2000coaching.domain.Meal;
import com.s2000coaching.domain.MealItem;
import com.s2000coaching.domain.MealKind;
import com.s2000coaching.domain.NutritionDayType;
import com.s2000coaching.domain.NutritionPlan;
import java.util.List;
import java.util.UUID;

/**
 * The skeleton a new nutrition plan starts from: the meals and supplement blocks the coach's
 * own plans are built on, with the foods already listed and every amount left empty. The coach
 * fills in the grams per member instead of retyping the same foods each time.
 *
 * <p>It is laid out twice, once for training days and once for rest days. The rest-day half is the
 * same day without the blocks that only make sense around a workout (the meals before and after it,
 * the post-workout supplements); the coach adds or removes blocks on either side.
 *
 * <p>Only names live here. Amounts, macros and the water target are per member and stay
 * blank, so a plan made from this scheme cannot be mistaken for a prescription until the
 * coach has actually written one.
 */
final class DefaultNutritionScheme {

	/** {@code aroundWorkout}: only meaningful on a training day, so the rest-day half leaves it out. */
	private record Block(MealKind kind, String name, List<String> foods, boolean aroundWorkout) {

		Block(MealKind kind, String name, List<String> foods) {
			this(kind, name, foods, false);
		}
	}

	private static final List<Block> BLOCKS = List.of(
			new Block(MealKind.MEAL, "Sabah", List.of(
					"Tam haşlanmış yumurta",
					"Maydonoz",
					"Tam buğday ekmeği veya yulaf ezmesi",
					"Mısır gevreği veya CocoPops",
					"Tam yağlı süt",
					"Badem")),
			new Block(MealKind.MEAL, "Öğlen", List.of(
					"Pirinç veya makarna veya patates",
					"Tavuk göğsü veya hindi göğüs",
					"Zeytinyağı",
					"Kapya biber ve soğan",
					"Havuç",
					"Yeşillikler")),
			new Block(MealKind.MEAL, "Antrenman öncesi", List.of(
					"Bebe bisküvisi",
					"Bal veya pekmez"), true),
			new Block(MealKind.MEAL, "Antrenman sonrası", List.of(
					"Muz",
					"Protein tozu"), true),
			new Block(MealKind.MEAL, "Akşam", List.of(
					"Pirinç veya makarna veya patates",
					"Tavuk pirzola, but, kıyma, kırmızı et veya balık",
					"Zeytinyağı",
					"Havuç",
					"Yeşillikler")),
			new Block(MealKind.MEAL, "Uyumadan önce", List.of(
					"Tam yağlı yoğurt",
					"Bebe bisküvisi",
					"Badem",
					"Bal veya pekmez")),
			new Block(MealKind.SUPPLEMENT, "Sabah kahvaltıdan sonra", List.of(
					"Omega 3 (EPA+DHA)",
					"Multivitamin",
					"C vitamini",
					"Magnezyum bisglisinat",
					"D3 vitamini")),
			new Block(MealKind.SUPPLEMENT, "Antrenman sonrası", List.of(
					"Whey protein",
					"Kreatin monohidrat"), true),
			new Block(MealKind.SUPPLEMENT, "Akşam öğünü ile beraber", List.of(
					"Omega 3 (EPA+DHA)",
					"Magnezyum bisglisinat",
					"ZMA")));

	private DefaultNutritionScheme() {
	}

	/** Appends the scheme to {@code plan} — the training-day half, then the rest-day half. */
	static void seed(NutritionPlan plan) {
		seed(plan, NutritionDayType.TRAINING);
		seed(plan, NutritionDayType.REST);
	}

	private static void seed(NutritionPlan plan, NutritionDayType dayType) {
		int order = plan.getMeals().stream().mapToInt(Meal::getDisplayOrder).max().orElse(0);
		for (Block block : BLOCKS) {
			if (dayType == NutritionDayType.REST && block.aroundWorkout()) {
				continue;
			}
			Meal meal = new Meal(UUID.randomUUID(), plan, ++order, block.name(), null, null);
			meal.setKind(block.kind());
			meal.setDayType(dayType);
			plan.getMeals().add(meal);

			int itemOrder = 0;
			for (String food : block.foods()) {
				meal.getItems().add(new MealItem(UUID.randomUUID(), meal, ++itemOrder, food, null, null,
						null, null, null, null));
			}
		}
	}
}
