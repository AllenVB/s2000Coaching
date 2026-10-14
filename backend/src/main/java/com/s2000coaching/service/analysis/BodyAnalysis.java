package com.s2000coaching.service.analysis;

import java.time.LocalDate;
import java.util.List;

/**
 * The computed result of a case study, shaped so that every figure carries the window it
 * covers and how many observations it rests on.
 *
 * <p>That is not decoration. A body-data set is usually uneven — two years of weigh-ins but
 * four months of food logs — and the easiest, most damaging misreading is taking a
 * maintenance-calorie number computed over four months as if it described the whole two
 * years. Each block below says what it saw, so the page can say it too.
 */
public record BodyAnalysis(
		Coverage coverage,
		List<DailyPoint> dailySeries,
		List<WeeklyRow> weeklyRows,
		LagFinding lagFinding,
		List<TdeeWindow> tdeeWindows,
		List<CalorieBucket> calorieBuckets,
		SodiumFinding sodiumFinding,
		List<Period> longTermPeriods,
		BodyComposition bodyComposition,
		List<String> caveats) {

	/** What data exists over which range, so nothing is read as covering more than it does. */
	public record Coverage(
			LocalDate measurementFirst,
			LocalDate measurementLast,
			int measurementDays,
			LocalDate intakeFirst,
			LocalDate intakeLast,
			int intakeDays,
			/** Days in the intake range with no entry — the gaps every average is blind to. */
			int intakeMissingDays,
			int sodiumDays,
			boolean hasNeck,
			boolean hasWaist) {
	}

	/** Raw weight next to its exponentially smoothed trend. */
	public record DailyPoint(LocalDate date, Double weightKg, Double trendWeightKg, Double waistCm,
			Integer calories) {
	}

	public record WeeklyRow(
			LocalDate weekStart,
			int intakeDaysLogged,
			Double avgCalories,
			Double avgProteinG,
			Double avgCarbsG,
			Double avgFatG,
			Double avgSodiumMg,
			Double trendWeightEnd,
			Double trendWeightDelta,
			Double waistDelta) {
	}

	/**
	 * Whether a week's calories line up better with that week's weight change or the next
	 * one's. Energy intake does not show on the scale the same day, and assuming no lag can
	 * make a real relationship look like noise.
	 */
	public record LagFinding(
			Integer sampleSize,
			Double correlationSameWeek,
			Double correlationNextWeek,
			String betterFit,
			String note) {
	}

	public record TdeeWindow(LocalDate windowStart, LocalDate windowEnd, int days, double avgCalories,
			double trendWeightChangeKg, double estimatedTdee) {
	}

	public record CalorieBucket(String label, int minCalories, Integer maxCalories, int weekCount,
			Double avgWeeklyWeightDelta, Double avgWeeklyWaistDelta, boolean gainedWithoutWaist) {
	}

	/** Explicitly exploratory: two weeks cannot settle anything. */
	public record SodiumFinding(Integer sampleSize, Double correlationWithNextDayWeight, String note) {
	}

	/** A stretch of the long record that moved one way: a bulk, a cut, or a flat spell. */
	public record Period(LocalDate start, LocalDate end, String label, double weightChangeKg,
			Double waistChangeCm, Double waistPerKg) {
	}

	/**
	 * An estimate, never a measurement. {@code method} names the formula and
	 * {@code uncertaintyPoints} the band around it, because a body-fat figure printed without
	 * either reads as fact.
	 */
	public record BodyComposition(
			String method,
			Double bodyFatPercent,
			Double leanMassKg,
			Double fatMassKg,
			double uncertaintyPoints,
			LocalDate asOf,
			String note) {
	}
}
