package com.s2000coaching.web.dto;

import com.s2000coaching.domain.WeeklyNutrition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A week of eating as it is shown: what was entered, the grams it works out to for the week, and
 * the daily average. The grams are calculated here, never stored or typed, so they cannot drift
 * from the calories and percentages they come from.
 */
public record WeeklyNutritionResponse(
		UUID id,
		LocalDate weekStart,
		LocalDate weekEnd,
		int calories,
		int carbsPercent,
		int fatPercent,
		int proteinPercent,
		/** The whole week. */
		Macros weekly,
		/** The week divided by seven. */
		Macros dailyAverage,
		boolean hasPhoto,
		Instant createdAt,
		Instant updatedAt) {

	/** Energy and grams; calories are the kcal, the rest are grams. */
	public record Macros(BigDecimal calories, BigDecimal carbsG, BigDecimal fatG, BigDecimal proteinG) {
	}

	public static final int DAYS = 7;
	private static final BigDecimal KCAL_PER_G_CARBS_PROTEIN = BigDecimal.valueOf(4);
	private static final BigDecimal KCAL_PER_G_FAT = BigDecimal.valueOf(9);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	public static WeeklyNutritionResponse from(WeeklyNutrition entry) {
		return new WeeklyNutritionResponse(
				entry.getId(),
				entry.getWeekStart(),
				entry.getWeekStart().plusDays(DAYS - 1),
				entry.getCalories(),
				entry.getCarbsPercent(),
				entry.getFatPercent(),
				entry.getProteinPercent(),
				macros(entry, 1),
				macros(entry, DAYS),
				entry.hasPhoto(),
				entry.getCreatedAt(),
				entry.getUpdatedAt());
	}

	/** The week's figures spread over {@code days} days, rounded to one decimal at the very end. */
	static Macros macros(WeeklyNutrition entry, int days) {
		BigDecimal kcal = BigDecimal.valueOf(entry.getCalories());
		return new Macros(
				round(kcal, days),
				round(grams(kcal, entry.getCarbsPercent(), KCAL_PER_G_CARBS_PROTEIN), days),
				round(grams(kcal, entry.getFatPercent(), KCAL_PER_G_FAT), days),
				round(grams(kcal, entry.getProteinPercent(), KCAL_PER_G_CARBS_PROTEIN), days));
	}

	private static BigDecimal grams(BigDecimal kcal, int percent, BigDecimal kcalPerGram) {
		return kcal.multiply(BigDecimal.valueOf(percent)).divide(HUNDRED).divide(kcalPerGram, 10, RoundingMode.HALF_UP);
	}

	private static BigDecimal round(BigDecimal value, int days) {
		return value.divide(BigDecimal.valueOf(days), 10, RoundingMode.HALF_UP).setScale(1, RoundingMode.HALF_UP);
	}
}
