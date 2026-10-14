package com.s2000coaching.service.analysis;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyIntake;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.service.analysis.BodyAnalysis.CalorieBucket;
import com.s2000coaching.service.analysis.BodyAnalysis.Coverage;
import com.s2000coaching.service.analysis.BodyAnalysis.DailyPoint;
import com.s2000coaching.service.analysis.BodyAnalysis.LagFinding;
import com.s2000coaching.service.analysis.BodyAnalysis.Period;
import com.s2000coaching.service.analysis.BodyAnalysis.SodiumFinding;
import com.s2000coaching.service.analysis.BodyAnalysis.TdeeWindow;
import com.s2000coaching.service.analysis.BodyAnalysis.WeeklyRow;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Turns a case study's raw rows into the published analysis.
 *
 * <p>The guiding rule throughout: a block that lacks the data to answer honestly returns
 * nothing and says why, rather than returning a number computed from too little. A
 * maintenance-calorie figure from three weeks, or a correlation from four points, looks
 * exactly as authoritative on a page as one from two years — which is what makes it
 * dangerous.
 */
public final class BodyAnalyzer {

	/** Energy in a kilogram of body mass. The usual working figure for this arithmetic. */
	private static final double KCAL_PER_KG = 7700;

	/** Below this a maintenance estimate is mostly water-weight noise. */
	private static final int MIN_TDEE_WINDOW_DAYS = 14;

	private static final int TDEE_WINDOW_DAYS = 21;
	private static final int TDEE_WINDOW_STEP_DAYS = 7;

	/** A week needs this many logged days before its average means anything. */
	private static final int MIN_LOGGED_DAYS_PER_WEEK = 4;

	/** Weekly trend movement that counts as a direction rather than drift. */
	private static final double PERIOD_THRESHOLD_KG_PER_WEEK = 0.1;

	private static final int MIN_PERIOD_WEEKS = 4;

	/** Fewer weigh-ins than this and "periods" would just be names given to noise. */
	private static final int MIN_PERIOD_MEASUREMENTS = 8;

	/**
	 * Below this the sodium section is marked thin outright. Three weeks is not a threshold
	 * at which it becomes conclusive — a water-retention effect needs controlled conditions,
	 * not a longer log — it is simply the point below which saying so is unavoidable.
	 */
	private static final int MIN_SODIUM_DAYS = 21;

	/** Half-width of the window a weigh-in's direction is read from. */
	private static final int PERIOD_WINDOW_DAYS = 21;

	private BodyAnalyzer() {
	}

	public static BodyAnalysis analyze(CaseStudy study, List<CaseStudyMeasurement> rawMeasurements,
			List<CaseStudyIntake> rawIntakes, int currentYear) {
		List<String> caveats = new ArrayList<>();

		// Sorted here rather than assumed. Every block below walks these in date order, and a
		// caller handing over an unordered list would produce a trend line that zig-zags
		// through time and periods dated backwards — without any of it failing visibly.
		List<CaseStudyMeasurement> measurements = rawMeasurements.stream()
				.sorted(Comparator.comparing(CaseStudyMeasurement::getMeasuredOn)).toList();
		List<CaseStudyIntake> intakes = rawIntakes.stream()
				.sorted(Comparator.comparing(CaseStudyIntake::getConsumedOn)).toList();

		var weightPoints = measurements.stream()
				.map(m -> new TrendCalculator.Observation(m.getMeasuredOn(), m.getWeightKg().doubleValue()))
				.toList();
		var trend = TrendCalculator.exponentialTrend(weightPoints, TrendCalculator.DAILY_ALPHA);
		Map<LocalDate, Double> trendByDate = new LinkedHashMap<>();
		trend.forEach(o -> trendByDate.put(o.date(), o.value()));

		Map<LocalDate, CaseStudyIntake> intakeByDate = new TreeMap<>();
		intakes.forEach(i -> intakeByDate.put(i.getConsumedOn(), i));

		Coverage coverage = buildCoverage(measurements, intakes);
		List<DailyPoint> daily = buildDailySeries(measurements, trendByDate, intakeByDate);
		List<WeeklyRow> weekly = buildWeeklyRows(measurements, trendByDate, intakeByDate, caveats);
		LagFinding lag = findLag(weekly);
		List<TdeeWindow> tdee = buildTdeeWindows(trendByDate, intakeByDate, caveats);
		List<CalorieBucket> buckets = buildCalorieBuckets(weekly);
		SodiumFinding sodium = exploreSodium(measurements, intakes);
		List<Period> periods = buildPeriods(measurements, trendByDate);
		var composition = estimateComposition(study, measurements, currentYear);

		addCoverageCaveats(coverage, caveats);
		return new BodyAnalysis(coverage, daily, weekly, lag, tdee, buckets, sodium, periods, composition,
				caveats);
	}

	private static Coverage buildCoverage(List<CaseStudyMeasurement> measurements,
			List<CaseStudyIntake> intakes) {
		LocalDate mFirst = measurements.isEmpty() ? null : measurements.get(0).getMeasuredOn();
		LocalDate mLast = measurements.isEmpty() ? null
				: measurements.get(measurements.size() - 1).getMeasuredOn();
		LocalDate iFirst = intakes.isEmpty() ? null : intakes.get(0).getConsumedOn();
		LocalDate iLast = intakes.isEmpty() ? null : intakes.get(intakes.size() - 1).getConsumedOn();

		// The gaps are what every average is blind to, so they are counted rather than implied.
		int missing = 0;
		if (iFirst != null) {
			int span = (int) ChronoUnit.DAYS.between(iFirst, iLast) + 1;
			missing = Math.max(0, span - intakes.size());
		}

		return new Coverage(mFirst, mLast, measurements.size(), iFirst, iLast, intakes.size(), missing,
				(int) intakes.stream().filter(i -> i.getSodiumMg() != null).count(),
				measurements.stream().anyMatch(m -> m.getNeckCm() != null),
				measurements.stream().anyMatch(m -> m.getWaistCm() != null));
	}

	private static List<DailyPoint> buildDailySeries(List<CaseStudyMeasurement> measurements,
			Map<LocalDate, Double> trendByDate, Map<LocalDate, CaseStudyIntake> intakeByDate) {
		List<DailyPoint> points = new ArrayList<>();
		for (CaseStudyMeasurement m : measurements) {
			LocalDate date = m.getMeasuredOn();
			CaseStudyIntake intake = intakeByDate.get(date);
			points.add(new DailyPoint(date, m.getWeightKg().doubleValue(),
					round(trendByDate.get(date)), decimal(m.getWaistCm()),
					intake == null ? null : intake.getCalories()));
		}
		return points;
	}

	/** Weeks start on Monday so that a "week" is the same stretch everywhere in the output. */
	private static List<WeeklyRow> buildWeeklyRows(List<CaseStudyMeasurement> measurements,
			Map<LocalDate, Double> trendByDate, Map<LocalDate, CaseStudyIntake> intakeByDate,
			List<String> caveats) {
		Map<LocalDate, List<CaseStudyIntake>> intakeByWeek = new TreeMap<>();
		intakeByDate.values().forEach(i -> intakeByWeek
				.computeIfAbsent(weekStart(i.getConsumedOn()), k -> new ArrayList<>()).add(i));

		Map<LocalDate, CaseStudyMeasurement> lastMeasurementOfWeek = new TreeMap<>();
		measurements.forEach(m -> lastMeasurementOfWeek.put(weekStart(m.getMeasuredOn()), m));

		List<WeeklyRow> rows = new ArrayList<>();
		Double previousTrend = null;
		LocalDate previousTrendWeek = null;
		Double previousWaist = null;
		LocalDate previousWaistWeek = null;
		int thinWeeks = 0;

		for (var entry : intakeByWeek.entrySet()) {
			LocalDate week = entry.getKey();
			List<CaseStudyIntake> days = entry.getValue();
			if (days.size() < MIN_LOGGED_DAYS_PER_WEEK) {
				// Kept out entirely: a week averaged from two days is not comparable with one
				// averaged from seven, and mixing them quietly biases every later figure.
				thinWeeks++;
				continue;
			}

			CaseStudyMeasurement endOfWeek = lastMeasurementOfWeek.get(week);
			Double trendEnd = endOfWeek == null ? null : trendByDate.get(endOfWeek.getMeasuredOn());
			Double waistEnd = endOfWeek == null ? null : decimal(endOfWeek.getWaistCm());

			// A delta is only reported when the figure it is measured against came from the
			// week immediately before. Skipped weeks leave gaps, and subtracting across one
			// would print a three-week change in a column headed "weekly".
			Double trendDelta = trendEnd != null && previousTrend != null
					&& follows(previousTrendWeek, week) ? trendEnd - previousTrend : null;
			Double waistDelta = waistEnd != null && previousWaist != null
					&& follows(previousWaistWeek, week) ? waistEnd - previousWaist : null;

			rows.add(new WeeklyRow(week, days.size(),
					round(average(days, i -> (double) i.getCalories())),
					round(average(days, i -> optional(i.getProteinG()))),
					round(average(days, i -> optional(i.getCarbsG()))),
					round(average(days, i -> optional(i.getFatG()))),
					round(average(days, i -> optional(i.getSodiumMg()))),
					round(trendEnd), round(trendDelta), round(waistDelta)));

			if (trendEnd != null) {
				previousTrend = trendEnd;
				previousTrendWeek = week;
			}
			if (waistEnd != null) {
				previousWaist = waistEnd;
				previousWaistWeek = week;
			}
		}

		if (thinWeeks > 0) {
			caveats.add("%d hafta, %d günden az kayıt içerdiği için haftalık tablodan çıkarıldı."
					.formatted(thinWeeks, MIN_LOGGED_DAYS_PER_WEEK));
		}
		return rows;
	}

	/**
	 * Compares a week's calories against that week's weight change and the next week's.
	 * Intake does not reach the scale the same day, and testing only the same week can make a
	 * real relationship look like noise.
	 */
	private static LagFinding findLag(List<WeeklyRow> weekly) {
		List<Double> calories = new ArrayList<>();
		List<Double> sameWeek = new ArrayList<>();
		List<Double> nextWeek = new ArrayList<>();

		for (int i = 0; i < weekly.size(); i++) {
			WeeklyRow row = weekly.get(i);
			if (row.avgCalories() == null || row.trendWeightDelta() == null) {
				continue;
			}
			// "Next week" has to actually be the next week; pairing across a gap in the table
			// would test this week's calories against a change from a month later.
			if (i + 1 < weekly.size() && weekly.get(i + 1).trendWeightDelta() != null
					&& follows(row.weekStart(), weekly.get(i + 1).weekStart())) {
				calories.add(row.avgCalories());
				sameWeek.add(row.trendWeightDelta());
				nextWeek.add(weekly.get(i + 1).trendWeightDelta());
			}
		}

		Double same = TrendCalculator.correlation(calories, sameWeek);
		Double next = TrendCalculator.correlation(calories, nextWeek);
		if (same == null && next == null) {
			return new LagFinding(calories.size(), null, null, null,
					"Karşılaştırma için en az 6 haftalık eşleşen veri gerekiyor; mevcut: %d."
							.formatted(calories.size()));
		}

		String better;
		if (same != null && (next == null || Math.abs(same) >= Math.abs(next))) {
			better = "AYNI_HAFTA";
		} else {
			better = "SONRAKI_HAFTA";
		}
		return new LagFinding(calories.size(), round(same), round(next), better,
				"Korelasyon %d haftalık eşleşen gözleme dayanıyor; nedensellik değil, birliktelik gösterir."
						.formatted(calories.size()));
	}

	/**
	 * Maintenance calories over rolling windows: average intake minus the energy the trend
	 * weight actually moved. Rolling rather than one figure because maintenance drifts as
	 * body mass and activity change — a single number for four months would hide that.
	 */
	private static List<TdeeWindow> buildTdeeWindows(Map<LocalDate, Double> trendByDate,
			Map<LocalDate, CaseStudyIntake> intakeByDate, List<String> caveats) {
		List<TdeeWindow> windows = new ArrayList<>();
		if (intakeByDate.isEmpty() || trendByDate.isEmpty()) {
			return windows;
		}

		List<LocalDate> intakeDates = new ArrayList<>(intakeByDate.keySet());
		LocalDate start = intakeDates.get(0);
		LocalDate lastDate = intakeDates.get(intakeDates.size() - 1);

		while (!start.plusDays(MIN_TDEE_WINDOW_DAYS - 1L).isAfter(lastDate)) {
			LocalDate end = start.plusDays(TDEE_WINDOW_DAYS - 1L);
			if (end.isAfter(lastDate)) {
				end = lastDate;
			}

			final LocalDate windowStart = start;
			final LocalDate windowEnd = end;
			List<CaseStudyIntake> inWindow = intakeByDate.entrySet().stream()
					.filter(e -> !e.getKey().isBefore(windowStart) && !e.getKey().isAfter(windowEnd))
					.map(Map.Entry::getValue)
					.toList();

			Double trendStart = nearestTrend(trendByDate, windowStart);
			Double trendEnd = nearestTrend(trendByDate, windowEnd);
			int spanDays = (int) ChronoUnit.DAYS.between(windowStart, windowEnd) + 1;

			// Needs most of the window logged: a 21-day window with 8 entries gives an average
			// that describes 8 days and a weight change that describes 21.
			if (trendStart != null && trendEnd != null && inWindow.size() >= spanDays * 0.6) {
				double avgCalories = inWindow.stream().mapToInt(CaseStudyIntake::getCalories).average()
						.orElse(0);
				double change = trendEnd - trendStart;
				double tdee = avgCalories - (change * KCAL_PER_KG / spanDays);
				windows.add(new TdeeWindow(windowStart, windowEnd, spanDays,
						TrendCalculator.round(avgCalories, 0), TrendCalculator.round(change, 2),
						TrendCalculator.round(tdee, 0)));
			}
			start = start.plusDays(TDEE_WINDOW_STEP_DAYS);
		}

		if (windows.isEmpty() && !intakeByDate.isEmpty()) {
			caveats.add("Bakım kalorisi hesaplanamadı: kalori kaydı ile aynı döneme denk gelen yeterli "
					+ "kilo ölçümü yok.");
		}
		return windows;
	}

	/** Buckets are cut from the data's own range, so they describe this record, not a textbook. */
	private static List<CalorieBucket> buildCalorieBuckets(List<WeeklyRow> weekly) {
		List<WeeklyRow> usable = weekly.stream()
				.filter(r -> r.avgCalories() != null && r.trendWeightDelta() != null)
				.toList();
		if (usable.size() < 4) {
			return List.of();
		}

		double min = usable.stream().mapToDouble(WeeklyRow::avgCalories).min().orElse(0);
		double max = usable.stream().mapToDouble(WeeklyRow::avgCalories).max().orElse(0);
		if (max - min < 100) {
			return List.of();
		}

		int bucketCount = usable.size() >= 12 ? 4 : 3;
		double width = (max - min) / bucketCount;
		List<CalorieBucket> buckets = new ArrayList<>();

		for (int b = 0; b < bucketCount; b++) {
			double lower = min + b * width;
			double upper = b == bucketCount - 1 ? Double.MAX_VALUE : min + (b + 1) * width;
			final double lo = lower;
			final double hi = upper;

			List<WeeklyRow> inBucket = usable.stream()
					.filter(r -> r.avgCalories() >= lo && r.avgCalories() < hi)
					.toList();
			if (inBucket.isEmpty()) {
				continue;
			}

			Double avgWeight = round(inBucket.stream().mapToDouble(WeeklyRow::trendWeightDelta).average()
					.orElse(0));
			List<WeeklyRow> withWaist = inBucket.stream().filter(r -> r.waistDelta() != null).toList();
			Double avgWaist = withWaist.isEmpty() ? null
					: round(withWaist.stream().mapToDouble(WeeklyRow::waistDelta).average().orElse(0));

			// The question the whole table exists to answer: is there a range where weight went
			// up without the waist following?
			boolean gainedClean = avgWeight != null && avgWeight > 0.05 && avgWaist != null
					&& avgWaist <= 0.05;

			buckets.add(new CalorieBucket(
					b == bucketCount - 1 ? "%.0f+ kcal".formatted(lower)
							: "%.0f-%.0f kcal".formatted(lower, upper),
					(int) Math.round(lower), b == bucketCount - 1 ? null : (int) Math.round(upper),
					inBucket.size(), avgWeight, avgWaist, gainedClean));
		}
		return buckets;
	}

	/**
	 * Sodium against the next day's raw weight — raw, not trend, because the effect being
	 * looked for is water, which is exactly what the trend is built to remove.
	 */
	private static SodiumFinding exploreSodium(List<CaseStudyMeasurement> measurements,
			List<CaseStudyIntake> intakes) {
		Map<LocalDate, Double> weightByDate = new TreeMap<>();
		measurements.forEach(m -> weightByDate.put(m.getMeasuredOn(), m.getWeightKg().doubleValue()));

		List<Double> sodium = new ArrayList<>();
		List<Double> nextDayChange = new ArrayList<>();
		for (CaseStudyIntake intake : intakes) {
			if (intake.getSodiumMg() == null) {
				continue;
			}
			Double today = weightByDate.get(intake.getConsumedOn());
			Double tomorrow = weightByDate.get(intake.getConsumedOn().plusDays(1));
			if (today != null && tomorrow != null) {
				sodium.add((double) intake.getSodiumMg());
				nextDayChange.add(tomorrow - today);
			}
		}

		if (sodium.isEmpty()) {
			return new SodiumFinding(0, null,
					"Sodyum kaydı ile aynı güne ve ertesi güne denk gelen tartı yok; bu bölüm boş.");
		}

		Double correlation = TrendCalculator.correlation(sodium, nextDayChange);
		String note = sodium.size() < MIN_SODIUM_DAYS
				? "Ön bulgu: yalnızca %d eşleşen gün var. Su tutma etkisi için kesin bir şey söylemek "
						.formatted(sodium.size()) + "mümkün değil."
				: "Ön bulgu: %d eşleşen gün. Sodyumun su dengesi üzerinden kiloyu geçici etkilediği "
						.formatted(sodium.size()) + "bilinir; bu veri onu doğrulamaya yetmez.";
		return new SodiumFinding(sodium.size(), round(correlation), note);
	}

	/**
	 * Splits the long record into stretches that moved one way: a bulk, a cut, a flat spell.
	 * Needs no intake data, so it covers the whole history rather than only the logged months.
	 *
	 * <p>Direction is read from a window centred on each weigh-in, not from the running
	 * average since the current stretch began. The difference is the whole point: a year of
	 * gaining followed by a year of cutting keeps a running average positive for months into
	 * the cut, so the turn would be dated long after it happened. A centred window dates it
	 * where it is.
	 *
	 * <p>Stretches too short to be a phase are then absorbed into the phase they interrupt.
	 * Three weeks of drift inside a six-month cut is noise, and listing it as a period of its
	 * own would turn one true story into five misleading ones.
	 */
	private static List<Period> buildPeriods(List<CaseStudyMeasurement> measurements,
			Map<LocalDate, Double> trendByDate) {
		if (measurements.size() < MIN_PERIOD_MEASUREMENTS) {
			return List.of();
		}
		long totalDays = ChronoUnit.DAYS.between(measurements.get(0).getMeasuredOn(),
				measurements.get(measurements.size() - 1).getMeasuredOn());
		if (totalDays < MIN_PERIOD_WEEKS * 7L) {
			return List.of();
		}

		List<String> labels = new ArrayList<>(measurements.size());
		for (int i = 0; i < measurements.size(); i++) {
			labels.add(localLabel(measurements, trendByDate, i));
		}

		List<Run> runs = mergeShortRuns(runsOf(labels), measurements);

		// Each period starts where the previous one ended, so the stretches chain instead of
		// leaving an unexplained gap between the last reading of one and the first of the next.
		List<int[]> ranges = new ArrayList<>();
		for (int i = 0; i < runs.size(); i++) {
			ranges.add(new int[] { i == 0 ? runs.get(i).from() : runs.get(i - 1).to(), runs.get(i).to() });
		}

		// The label is then re-derived from the stretch's own net change — the very number the
		// period publishes — and neighbours that come out the same are joined. Without this a
		// period could be headed "flat" while reporting a 1.2 kg loss underneath, because the
		// window that found the boundary and the figure on the page were measuring different
		// things.
		boolean joined = true;
		while (joined && ranges.size() > 1) {
			joined = false;
			for (int i = 0; i + 1 < ranges.size(); i++) {
				if (netLabel(measurements, trendByDate, ranges.get(i))
						.equals(netLabel(measurements, trendByDate, ranges.get(i + 1)))) {
					ranges.set(i, new int[] { ranges.get(i)[0], ranges.get(i + 1)[1] });
					ranges.remove(i + 1);
					joined = true;
					break;
				}
			}
		}

		List<Period> periods = new ArrayList<>();
		for (int[] range : ranges) {
			periods.add(toPeriod(measurements, range[0], range[1], trendByDate,
					netLabel(measurements, trendByDate, range)));
		}
		return periods;
	}

	/** The label a stretch earns from the net change it will print. */
	private static String netLabel(List<CaseStudyMeasurement> measurements,
			Map<LocalDate, Double> trendByDate, int[] range) {
		LocalDate from = measurements.get(range[0]).getMeasuredOn();
		LocalDate to = measurements.get(range[1]).getMeasuredOn();
		Double trendFrom = trendByDate.get(from);
		Double trendTo = trendByDate.get(to);
		if (trendFrom == null || trendTo == null) {
			return "SABIT";
		}
		long days = Math.max(1, ChronoUnit.DAYS.between(from, to));
		return classify((trendTo - trendFrom) / days * 7);
	}

	private static String classify(double kgPerWeek) {
		return kgPerWeek > PERIOD_THRESHOLD_KG_PER_WEEK ? "KILO_ALMA"
				: kgPerWeek < -PERIOD_THRESHOLD_KG_PER_WEEK ? "KILO_VERME" : "SABIT";
	}

	/** One stretch of equally-labelled weigh-ins, by index into the sorted list. */
	private record Run(int from, int to, String label) {
	}

	/**
	 * Which way the trend was moving around one weigh-in, from the readings within
	 * {@link #PERIOD_WINDOW_DAYS} either side of it.
	 */
	private static String localLabel(List<CaseStudyMeasurement> measurements,
			Map<LocalDate, Double> trendByDate, int index) {
		LocalDate centre = measurements.get(index).getMeasuredOn();
		List<Double> days = new ArrayList<>();
		List<Double> values = new ArrayList<>();
		for (CaseStudyMeasurement m : measurements) {
			long offset = ChronoUnit.DAYS.between(centre, m.getMeasuredOn());
			if (Math.abs(offset) <= PERIOD_WINDOW_DAYS && trendByDate.get(m.getMeasuredOn()) != null) {
				days.add((double) offset);
				values.add(trendByDate.get(m.getMeasuredOn()));
			}
		}

		// A lone reading in the window says nothing about direction, so the immediate
		// neighbours are used instead — however far away they are.
		if (days.size() < 2) {
			days.clear();
			values.clear();
			for (int i = Math.max(0, index - 1); i <= Math.min(measurements.size() - 1, index + 1); i++) {
				LocalDate date = measurements.get(i).getMeasuredOn();
				if (trendByDate.get(date) != null) {
					days.add((double) ChronoUnit.DAYS.between(centre, date));
					values.add(trendByDate.get(date));
				}
			}
		}

		Double perDay = TrendCalculator.slope(days, values);
		return perDay == null ? "SABIT" : classify(perDay * 7);
	}

	private static List<Run> runsOf(List<String> labels) {
		List<Run> runs = new ArrayList<>();
		int start = 0;
		for (int i = 1; i <= labels.size(); i++) {
			if (i == labels.size() || !labels.get(i).equals(labels.get(start))) {
				runs.add(new Run(start, i - 1, labels.get(start)));
				start = i;
			}
		}
		return runs;
	}

	/**
	 * Repeatedly folds the shortest under-length stretch into a neighbour until every
	 * remaining stretch is at least {@link #MIN_PERIOD_WEEKS} long, or only one is left.
	 */
	private static List<Run> mergeShortRuns(List<Run> runs, List<CaseStudyMeasurement> measurements) {
		List<Run> merged = new ArrayList<>(runs);
		while (merged.size() > 1) {
			int shortest = -1;
			long shortestDays = Long.MAX_VALUE;
			for (int i = 0; i < merged.size(); i++) {
				long days = spanDays(measurements, merged.get(i));
				if (days < MIN_PERIOD_WEEKS * 7L && days < shortestDays) {
					shortest = i;
					shortestDays = days;
				}
			}
			if (shortest < 0) {
				break;
			}

			int neighbour = pickNeighbour(merged, measurements, shortest);
			int low = Math.min(shortest, neighbour);
			int high = Math.max(shortest, neighbour);
			// The neighbour's label survives: the short stretch is an interruption in it, not
			// a phase that the longer one interrupted.
			merged.set(low, new Run(merged.get(low).from(), merged.get(high).to(),
					merged.get(neighbour).label()));
			merged.remove(high);
		}
		return merged;
	}

	/** The longer neighbour, since that is the phase the short stretch sits inside. */
	private static int pickNeighbour(List<Run> runs, List<CaseStudyMeasurement> measurements,
			int index) {
		if (index == 0) {
			return 1;
		}
		if (index == runs.size() - 1) {
			return index - 1;
		}
		return spanDays(measurements, runs.get(index - 1)) >= spanDays(measurements, runs.get(index + 1))
				? index - 1
				: index + 1;
	}

	private static long spanDays(List<CaseStudyMeasurement> measurements, Run run) {
		return ChronoUnit.DAYS.between(measurements.get(run.from()).getMeasuredOn(),
				measurements.get(run.to()).getMeasuredOn());
	}

	private static Period toPeriod(List<CaseStudyMeasurement> sorted, int fromIndex, int toIndex,
			Map<LocalDate, Double> trendByDate, String label) {
		CaseStudyMeasurement from = sorted.get(fromIndex);
		CaseStudyMeasurement to = sorted.get(toIndex);
		double weightChange = trendByDate.get(to.getMeasuredOn()) - trendByDate.get(from.getMeasuredOn());

		Double waistChange = from.getWaistCm() != null && to.getWaistCm() != null
				? to.getWaistCm().subtract(from.getWaistCm()).doubleValue()
				: null;
		// Waist centimetres gained per kilogram: a rough read on what the weight was made of.
		Double waistPerKg = waistChange != null && Math.abs(weightChange) > 0.5
				? round(waistChange / weightChange)
				: null;

		return new Period(from.getMeasuredOn(), to.getMeasuredOn(), label,
				TrendCalculator.round(weightChange, 1), round(waistChange), waistPerKg);
	}

	private static BodyAnalysis.BodyComposition estimateComposition(CaseStudy study,
			List<CaseStudyMeasurement> measurements, int currentYear) {
		if (measurements.isEmpty()) {
			return BodyCompositionEstimator.estimate(null, null, study.getHeightCm(), study.getSex(),
					study.getBirthYear(), null, null, null, currentYear);
		}

		// The most recent row that carries the measurements the formula needs — the latest
		// weigh-in may have no tape numbers at all.
		CaseStudyMeasurement best = measurements.get(measurements.size() - 1);
		for (int i = measurements.size() - 1; i >= 0; i--) {
			CaseStudyMeasurement candidate = measurements.get(i);
			if (candidate.getNeckCm() != null && candidate.getWaistCm() != null) {
				best = candidate;
				break;
			}
		}

		return BodyCompositionEstimator.estimate(best.getMeasuredOn(), best.getWeightKg().doubleValue(),
				study.getHeightCm(), study.getSex(), study.getBirthYear(), decimal(best.getNeckCm()),
				decimal(best.getWaistCm()), decimal(best.getHipCm()), currentYear);
	}

	private static void addCoverageCaveats(Coverage coverage, List<String> caveats) {
		if (coverage.intakeDays() == 0) {
			caveats.add("Kalori kaydı yok: bakım kalorisi, kalori-kilo ilişkisi ve kalori aralığı "
					+ "tablosu hesaplanamaz. Yalnızca uzun dönem kilo ve çevre ölçümü analizi var.");
		} else if (coverage.intakeMissingDays() > 0) {
			caveats.add("Kalori kaydının kapsadığı aralıkta %d gün boş; haftalık ortalamalar yalnızca "
					.formatted(coverage.intakeMissingDays()) + "kayıt girilen günleri temsil eder.");
		}
		if (!coverage.hasNeck()) {
			caveats.add("Boyun ölçüsü yok: yağ oranı daha dar hata paylı Navy formülü yerine BKİ "
					+ "tabanlı formülle tahmin edilir.");
		}
		if (coverage.sodiumDays() > 0 && coverage.sodiumDays() < MIN_SODIUM_DAYS) {
			caveats.add("Sodyum verisi yalnızca %d gün; bu bölüm kesin sonuç değil ön bulgudur."
					.formatted(coverage.sodiumDays()));
		}
	}

	private static LocalDate weekStart(LocalDate date) {
		return date.with(DayOfWeek.MONDAY);
	}

	/** True when {@code week} is the calendar week straight after {@code previousWeek}. */
	private static boolean follows(LocalDate previousWeek, LocalDate week) {
		return previousWeek != null && previousWeek.plusWeeks(1).equals(week);
	}

	private static Double nearestTrend(Map<LocalDate, Double> trendByDate, LocalDate target) {
		Double exact = trendByDate.get(target);
		if (exact != null) {
			return exact;
		}
		// The nearest weigh-in within a few days: a window edge rarely lands on one.
		for (int offset = 1; offset <= 4; offset++) {
			Double before = trendByDate.get(target.minusDays(offset));
			if (before != null) {
				return before;
			}
			Double after = trendByDate.get(target.plusDays(offset));
			if (after != null) {
				return after;
			}
		}
		return null;
	}

	private static <T> Double average(List<T> items, Function<T, Double> extractor) {
		List<Double> values = items.stream().map(extractor).filter(v -> v != null).toList();
		return values.isEmpty() ? null
				: values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
	}

	private static Double optional(Integer value) {
		return value == null ? null : value.doubleValue();
	}

	private static Double decimal(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	private static Double round(Double value) {
		return value == null ? null : TrendCalculator.round(value, 2);
	}
}
