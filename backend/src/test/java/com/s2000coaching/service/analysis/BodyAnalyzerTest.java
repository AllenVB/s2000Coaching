package com.s2000coaching.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyIntake;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.domain.Sex;
import com.s2000coaching.service.analysis.BodyAnalysis.CalorieBucket;
import com.s2000coaching.service.analysis.BodyAnalysis.Period;
import com.s2000coaching.service.analysis.BodyAnalysis.WeeklyRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests built on synthetic data shaped like the real record this will run on: roughly two
 * years of weekly weigh-ins, four recent months of daily food logs, and sodium for only a
 * fortnight.
 *
 * <p>What is checked is mostly not "is the number right" but "does the engine keep quiet when
 * it cannot know". A plausible-looking maintenance figure computed from three logged days is
 * the failure mode that matters here, because nothing about it looks wrong on the page.
 */
class BodyAnalyzerTest {

	/** A Monday, so that "week" boundaries in the fixtures line up with the engine's. */
	private static final LocalDate MONDAY = LocalDate.of(2025, 1, 6);

	private static final int CURRENT_YEAR = 2026;

	private CaseStudy study;

	private CaseStudy newStudy() {
		return new CaseStudy(UUID.randomUUID(), "kendi-verim", "Kendi Verim", Instant.now());
	}

	private CaseStudy studyWithBody(Integer heightCm, Sex sex, Integer birthYear) {
		CaseStudy created = newStudy();
		created.setHeightCm(heightCm);
		created.setSex(sex);
		created.setBirthYear(birthYear);
		return created;
	}

	private CaseStudyMeasurement weigh(LocalDate date, double kg) {
		return new CaseStudyMeasurement(UUID.randomUUID(), study, date,
				BigDecimal.valueOf(kg).setScale(2, java.math.RoundingMode.HALF_UP));
	}

	private CaseStudyMeasurement weigh(LocalDate date, double kg, Double waistCm) {
		CaseStudyMeasurement m = weigh(date, kg);
		if (waistCm != null) {
			m.setWaistCm(BigDecimal.valueOf(waistCm));
		}
		return m;
	}

	private CaseStudyIntake ate(LocalDate date, int kcal) {
		return new CaseStudyIntake(UUID.randomUUID(), study, date, kcal);
	}

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		study = newStudy();
	}

	// --- fixtures -----------------------------------------------------------------------

	/** Two years of weekly weigh-ins: 40 weeks up, 40 weeks down, then flat. */
	private List<CaseStudyMeasurement> twoYearsWeekly() {
		List<CaseStudyMeasurement> measurements = new ArrayList<>();
		double weight = 78.0;
		for (int week = 0; week < 104; week++) {
			measurements.add(weigh(MONDAY.plusWeeks(week), weight, 84.0));
			weight += week < 40 ? 0.15 : week < 80 ? -0.20 : 0.0;
		}
		return measurements;
	}

	/** Daily weigh-ins and daily logs over {@code weeks}, at a steady deficit. */
	private List<CaseStudyMeasurement> dailyWeighIns(LocalDate from, int days, double startKg,
			double kgPerDay) {
		List<CaseStudyMeasurement> measurements = new ArrayList<>();
		for (int day = 0; day < days; day++) {
			measurements.add(weigh(from.plusDays(day), startKg + kgPerDay * day, 84.0));
		}
		return measurements;
	}

	private List<CaseStudyIntake> dailyIntake(LocalDate from, int days, int kcal) {
		List<CaseStudyIntake> intakes = new ArrayList<>();
		for (int day = 0; day < days; day++) {
			intakes.add(ate(from.plusDays(day), kcal));
		}
		return intakes;
	}

	// --- tests --------------------------------------------------------------------------

	@Nested
	class CoverageBlock {

		@Test
		void shouldReportTheTwoWindowsSeparately() {
			List<CaseStudyMeasurement> measurements = twoYearsWeekly();
			LocalDate intakeStart = MONDAY.plusWeeks(88);
			List<CaseStudyIntake> intakes = dailyIntake(intakeStart, 120, 2400);

			var coverage = BodyAnalyzer.analyze(study, measurements, intakes, CURRENT_YEAR).coverage();

			assertThat(coverage.measurementFirst()).isEqualTo(MONDAY);
			assertThat(coverage.measurementLast()).isEqualTo(MONDAY.plusWeeks(103));
			assertThat(coverage.measurementDays()).isEqualTo(104);
			assertThat(coverage.intakeFirst()).isEqualTo(intakeStart);
			assertThat(coverage.intakeLast()).isEqualTo(intakeStart.plusDays(119));
			assertThat(coverage.intakeDays()).isEqualTo(120);
			assertThat(coverage.intakeMissingDays()).isZero();
		}

		/** The gaps are the thing a four-month average is blind to, so they get counted. */
		@Test
		void shouldCountUnloggedDaysInsideTheIntakeRange() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int day = 0; day < 30; day++) {
				if (day % 5 != 0) {
					intakes.add(ate(MONDAY.plusDays(day), 2400));
				}
			}

			var coverage = BodyAnalyzer.analyze(study, List.of(), intakes, CURRENT_YEAR).coverage();

			assertThat(coverage.intakeDays()).isEqualTo(24);
			// Day 0 was skipped, so the range runs day 1 to day 29: 29 days, 24 of them logged.
			// Gaps before the first entry are not "missing" — the log had not started.
			assertThat(coverage.intakeMissingDays()).isEqualTo(5);
		}

		@Test
		void shouldSayWhetherTapeMeasurementsExist() {
			CaseStudyMeasurement withNeck = weigh(MONDAY, 80.0, 84.0);
			withNeck.setNeckCm(BigDecimal.valueOf(38.0));

			var withBoth = BodyAnalyzer.analyze(study, List.of(withNeck), List.of(), CURRENT_YEAR)
					.coverage();
			var weightOnly = BodyAnalyzer.analyze(study, List.of(weigh(MONDAY, 80.0)), List.of(),
					CURRENT_YEAR).coverage();

			assertThat(withBoth.hasNeck()).isTrue();
			assertThat(withBoth.hasWaist()).isTrue();
			assertThat(weightOnly.hasNeck()).isFalse();
			assertThat(weightOnly.hasWaist()).isFalse();
		}
	}

	@Nested
	class WeeklyTable {

		@Test
		void shouldExcludeWeeksWithTooFewLoggedDaysAndSayHowMany() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			// Week 1: fully logged. Week 2: two days only. Week 3: fully logged.
			for (int day = 0; day < 7; day++) {
				intakes.add(ate(MONDAY.plusDays(day), 2400));
				intakes.add(ate(MONDAY.plusWeeks(2).plusDays(day), 2600));
			}
			intakes.add(ate(MONDAY.plusWeeks(1), 2500));
			intakes.add(ate(MONDAY.plusWeeks(1).plusDays(1), 2500));

			var analysis = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 21, 80.0, -0.03), intakes,
					CURRENT_YEAR);

			assertThat(analysis.weeklyRows()).extracting(WeeklyRow::weekStart)
					.containsExactly(MONDAY, MONDAY.plusWeeks(2));
			assertThat(analysis.caveats())
					.anyMatch(c -> c.contains("1 hafta") && c.contains("4 günden az"));
		}

		/**
		 * The column is headed "weekly", so it must not quietly contain a two-week change
		 * whenever a week in between was dropped.
		 */
		@Test
		void shouldNotReportADeltaAcrossASkippedWeek() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int day = 0; day < 7; day++) {
				intakes.add(ate(MONDAY.plusDays(day), 2400));
				intakes.add(ate(MONDAY.plusWeeks(2).plusDays(day), 2400));
				intakes.add(ate(MONDAY.plusWeeks(3).plusDays(day), 2400));
			}

			var rows = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 28, 80.0, -0.03), intakes,
					CURRENT_YEAR).weeklyRows();

			assertThat(rows).hasSize(3);
			assertThat(rows.get(0).trendWeightDelta()).isNull();
			// Week 2 follows a gap, so it has nothing legitimate to subtract from.
			assertThat(rows.get(1).weekStart()).isEqualTo(MONDAY.plusWeeks(2));
			assertThat(rows.get(1).trendWeightDelta()).isNull();
			assertThat(rows.get(2).trendWeightDelta()).isNotNull();
		}

		@Test
		void shouldAverageOnlyTheMacrosThatWereLogged() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int day = 0; day < 7; day++) {
				CaseStudyIntake intake = ate(MONDAY.plusDays(day), 2400);
				if (day < 3) {
					intake.setProteinG(180);
				}
				intakes.add(intake);
			}

			var rows = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 7, 80.0, 0), intakes,
					CURRENT_YEAR).weeklyRows();

			assertThat(rows.get(0).avgCalories()).isEqualTo(2400.0);
			assertThat(rows.get(0).avgProteinG()).isEqualTo(180.0);
			assertThat(rows.get(0).avgCarbsG()).isNull();
		}

		@Test
		void shouldBeEmptyWithNoIntakeAtAll() {
			var analysis = BodyAnalyzer.analyze(study, twoYearsWeekly(), List.of(), CURRENT_YEAR);

			assertThat(analysis.weeklyRows()).isEmpty();
			assertThat(analysis.caveats()).anyMatch(c -> c.startsWith("Kalori kaydı yok"));
		}
	}

	@Nested
	class Tdee {

		/**
		 * The engine's central claim. Intake is 2200 and the trend falls 0.3 kg a week, so
		 * maintenance is 2200 + 0.3 × 7700 / 7 = 2530.
		 *
		 * <p>Settled windows land near 2514, about 16 kcal low. That is not a bug but the
		 * price of smoothing: the trend line lags the raw series, so the change measured
		 * across a window edge is slightly smaller than the real one. The alternative —
		 * reading raw weights — would swing the figure by hundreds of kcal on water weight.
		 */
		@Test
		void shouldRecoverMaintenanceCaloriesFromASteadyDeficit() {
			var measurements = dailyWeighIns(MONDAY, 120, 80.0, -0.3 / 7);
			var intakes = dailyIntake(MONDAY, 120, 2200);

			var windows = BodyAnalyzer.analyze(study, measurements, intakes, CURRENT_YEAR).tdeeWindows();

			assertThat(windows).isNotEmpty();
			var settled = windows.stream()
					.filter(w -> ChronoUnit.DAYS.between(MONDAY, w.windowStart()) >= 42)
					.toList();
			assertThat(settled).isNotEmpty();
			assertThat(settled).allSatisfy(w -> {
				assertThat(w.estimatedTdee()).isCloseTo(2530, within(40.0));
				assertThat(w.avgCalories()).isEqualTo(2200.0);
				assertThat(w.trendWeightChangeKg()).isLessThan(0);
			});
		}

		@Test
		void shouldRollForwardRatherThanGiveOneFigure() {
			var windows = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 60, 80.0, -0.03),
					dailyIntake(MONDAY, 60, 2300), CURRENT_YEAR).tdeeWindows();

			assertThat(windows).hasSizeGreaterThan(4);
			assertThat(windows.get(0).windowStart()).isEqualTo(MONDAY);
			assertThat(windows.get(1).windowStart()).isEqualTo(MONDAY.plusDays(7));
			assertThat(windows).allSatisfy(w -> assertThat(w.days())
					.isGreaterThanOrEqualTo(14).isLessThanOrEqualTo(21));
		}

		/** A 21-day window with a handful of logged days must produce nothing, not a figure. */
		@Test
		void shouldRefuseWindowsThatAreMostlyUnlogged() {
			List<CaseStudyIntake> sparse = new ArrayList<>();
			for (int day = 0; day < 60; day += 4) {
				sparse.add(ate(MONDAY.plusDays(day), 2300));
			}

			var analysis = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 60, 80.0, -0.03), sparse,
					CURRENT_YEAR);

			assertThat(analysis.tdeeWindows()).isEmpty();
			assertThat(analysis.caveats()).anyMatch(c -> c.startsWith("Bakım kalorisi hesaplanamadı"));
		}

		/** Logs from a stretch with no weigh-ins cannot produce a maintenance figure. */
		@Test
		void shouldRefuseWhenNoWeighInsOverlapTheLoggedDays() {
			var analysis = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 30, 80.0, -0.03),
					dailyIntake(MONDAY.plusYears(1), 60, 2300), CURRENT_YEAR);

			assertThat(analysis.tdeeWindows()).isEmpty();
			assertThat(analysis.caveats()).anyMatch(c -> c.startsWith("Bakım kalorisi hesaplanamadı"));
		}

		@Test
		void shouldRefuseALoggedStretchShorterThanTwoWeeks() {
			var windows = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 10, 80.0, -0.03),
					dailyIntake(MONDAY, 10, 2300), CURRENT_YEAR).tdeeWindows();

			assertThat(windows).isEmpty();
		}
	}

	@Nested
	class Lag {

		@Test
		void shouldRefuseToCorrelateFewerThanSixMatchedWeeks() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int week = 0; week < 5; week++) {
				for (int day = 0; day < 7; day++) {
					intakes.add(ate(MONDAY.plusWeeks(week).plusDays(day), 2200 + week * 150));
				}
			}

			var lag = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 35, 80.0, -0.02), intakes,
					CURRENT_YEAR).lagFinding();

			assertThat(lag.correlationSameWeek()).isNull();
			assertThat(lag.correlationNextWeek()).isNull();
			assertThat(lag.betterFit()).isNull();
			assertThat(lag.note()).contains("en az 6").contains("mevcut: " + lag.sampleSize());
		}

		@Test
		void shouldCompareBothLagsAndStateTheSampleSize() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int week = 0; week < 14; week++) {
				int kcal = week % 2 == 0 ? 2000 : 3200;
				for (int day = 0; day < 7; day++) {
					intakes.add(ate(MONDAY.plusWeeks(week).plusDays(day), kcal));
				}
			}

			var lag = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 98, 80.0, 0.01), intakes,
					CURRENT_YEAR).lagFinding();

			assertThat(lag.sampleSize()).isGreaterThanOrEqualTo(6);
			assertThat(lag.correlationSameWeek()).isNotNull();
			assertThat(lag.correlationNextWeek()).isNotNull();
			assertThat(lag.betterFit()).isIn("AYNI_HAFTA", "SONRAKI_HAFTA");
			assertThat(lag.note()).contains("nedensellik değil");
		}
	}

	@Nested
	class CalorieBuckets {

		@Test
		void shouldStayEmptyWithTooFewWeeks() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int week = 0; week < 3; week++) {
				for (int day = 0; day < 7; day++) {
					intakes.add(ate(MONDAY.plusWeeks(week).plusDays(day), 2000 + week * 400));
				}
			}

			assertThat(BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 21, 80.0, 0.01), intakes,
					CURRENT_YEAR).calorieBuckets()).isEmpty();
		}

		@Test
		void shouldStayEmptyWhenCaloriesBarelyVary() {
			List<CaseStudyIntake> intakes = new ArrayList<>();
			for (int week = 0; week < 12; week++) {
				for (int day = 0; day < 7; day++) {
					intakes.add(ate(MONDAY.plusWeeks(week).plusDays(day), 2400 + week));
				}
			}

			assertThat(BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 84, 80.0, 0.01), intakes,
					CURRENT_YEAR).calorieBuckets()).isEmpty();
		}

		/**
		 * The question the table exists to answer: a range where the scale went up and the
		 * waist did not.
		 */
		@Test
		void shouldFlagARangeWhereWeightRoseWithoutTheWaist() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>();
			List<CaseStudyIntake> intakes = new ArrayList<>();
			double weight = 78.0;
			for (int day = 0; day < 112; day++) {
				boolean surplus = day < 56;
				measurements.add(weigh(MONDAY.plusDays(day), weight, 84.0));
				intakes.add(ate(MONDAY.plusDays(day), surplus ? 3400 : 2200));
				weight += surplus ? 0.2 / 7 : -0.4 / 7;
			}

			var buckets = BodyAnalyzer.analyze(study, measurements, intakes, CURRENT_YEAR)
					.calorieBuckets();

			assertThat(buckets).isNotEmpty();
			CalorieBucket highest = buckets.get(buckets.size() - 1);
			assertThat(highest.minCalories()).isGreaterThan(3000);
			assertThat(highest.maxCalories()).isNull();
			assertThat(highest.avgWeeklyWeightDelta()).isGreaterThan(0);
			assertThat(highest.avgWeeklyWaistDelta()).isEqualTo(0.0);
			assertThat(highest.gainedWithoutWaist()).isTrue();

			CalorieBucket lowest = buckets.get(0);
			assertThat(lowest.avgWeeklyWeightDelta()).isLessThan(0);
			assertThat(lowest.gainedWithoutWaist()).isFalse();
			assertThat(buckets).allSatisfy(b -> assertThat(b.weekCount()).isPositive());
		}
	}

	@Nested
	class Sodium {

		@Test
		void shouldSayNothingWhenNoSodiumWasLogged() {
			var finding = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 30, 80.0, 0),
					dailyIntake(MONDAY, 30, 2400), CURRENT_YEAR).sodiumFinding();

			assertThat(finding.sampleSize()).isZero();
			assertThat(finding.correlationWithNextDayWeight()).isNull();
			assertThat(finding.note()).contains("bu bölüm boş");
		}

		/** A fortnight is all this record has, so the section must read as exploratory. */
		@Test
		void shouldMarkAFortnightAsPreliminary() {
			List<CaseStudyIntake> intakes = dailyIntake(MONDAY, 30, 2400);
			for (int day = 0; day < 14; day++) {
				intakes.get(day).setSodiumMg(2000 + day * 200);
			}

			var analysis = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 30, 80.0, 0), intakes,
					CURRENT_YEAR);

			assertThat(analysis.sodiumFinding().sampleSize()).isEqualTo(14);
			assertThat(analysis.sodiumFinding().note()).startsWith("Ön bulgu");
			assertThat(analysis.caveats()).anyMatch(c -> c.contains("ön bulgudur"));
		}

		@Test
		void shouldNeedBothDaysWeighed() {
			List<CaseStudyIntake> intakes = dailyIntake(MONDAY, 10, 2400);
			intakes.forEach(i -> i.setSodiumMg(3000));

			var finding = BodyAnalyzer.analyze(study,
					List.of(weigh(MONDAY, 80.0), weigh(MONDAY.plusDays(5), 80.4)), intakes, CURRENT_YEAR)
					.sodiumFinding();

			assertThat(finding.sampleSize()).isZero();
		}
	}

	@Nested
	class Periods {

		@Test
		void shouldSplitTwoYearsIntoGainCutAndFlat() {
			List<Period> periods = BodyAnalyzer.analyze(study, twoYearsWeekly(), List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).extracting(Period::label)
					.containsExactly("KILO_ALMA", "KILO_VERME", "SABIT");
			assertThat(periods.get(0).weightChangeKg()).isPositive();
			assertThat(periods.get(1).weightChangeKg()).isNegative();
			assertThat(periods.get(2).weightChangeKg()).isCloseTo(0, within(0.3));
		}

		/**
		 * The reason direction is read from a centred window: the turn has to be dated where
		 * it happened, not months later once a running average finally flips.
		 */
		@Test
		void shouldDateTheTurningPointCloseToWhereItHappened() {
			LocalDate trueTurn = MONDAY.plusWeeks(40);

			List<Period> periods = BodyAnalyzer.analyze(study, twoYearsWeekly(), List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(Math.abs(ChronoUnit.DAYS.between(trueTurn, periods.get(0).end())))
					.isLessThanOrEqualTo(14);
		}

		@Test
		void shouldChainWithoutGaps() {
			List<Period> periods = BodyAnalyzer.analyze(study, twoYearsWeekly(), List.of(), CURRENT_YEAR)
					.longTermPeriods();

			for (int i = 1; i < periods.size(); i++) {
				assertThat(periods.get(i).start()).isEqualTo(periods.get(i - 1).end());
			}
		}

		@Test
		void shouldReportNoPeriodsForAShortRecord() {
			assertThat(BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 20, 80.0, 0.01), List.of(),
					CURRENT_YEAR).longTermPeriods()).isEmpty();
		}

		/** One heavy week inside a long cut is noise, not a phase of its own. */
		@Test
		void shouldAbsorbASingleBadWeekIntoTheSurroundingPhase() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>();
			double weight = 86.0;
			for (int week = 0; week < 40; week++) {
				measurements.add(weigh(MONDAY.plusWeeks(week), weight + (week == 20 ? 0.4 : 0.0), 92.0));
				weight -= 0.3;
			}

			List<Period> periods = BodyAnalyzer.analyze(study, measurements, List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).extracting(Period::label).containsExactly("KILO_VERME");
		}

		/**
		 * A plateau, unlike a wobble, is worth naming: two months where a cut stopped moving
		 * is the most useful thing in the record, and smoothing it into the cut either side
		 * would erase it.
		 */
		@Test
		void shouldReportAGenuinePlateauInsideACut() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>();
			double weight = 86.0;
			for (int week = 0; week < 40; week++) {
				measurements.add(weigh(MONDAY.plusWeeks(week), weight, 92.0));
				weight -= week >= 16 && week < 24 ? 0.0 : 0.3;
			}

			List<Period> periods = BodyAnalyzer.analyze(study, measurements, List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).extracting(Period::label)
					.containsExactly("KILO_VERME", "SABIT", "KILO_VERME");
			assertThat(Math.abs(ChronoUnit.DAYS.between(MONDAY.plusWeeks(16), periods.get(1).start())))
					.isLessThanOrEqualTo(14);
		}

		/**
		 * The label and the number next to it must come from the same measurement. A period
		 * headed "flat" that reports a 1.2 kg loss underneath is worse than no period at all.
		 */
		@Test
		void shouldLabelEveryPeriodConsistentlyWithItsOwnReportedChange() {
			List<Period> periods = BodyAnalyzer.analyze(study, twoYearsWeekly(), List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).isNotEmpty();
			assertThat(periods).allSatisfy(period -> {
				double perWeek = period.weightChangeKg()
						/ Math.max(1, ChronoUnit.DAYS.between(period.start(), period.end())) * 7;
				switch (period.label()) {
					case "KILO_ALMA" -> assertThat(perWeek).isGreaterThan(0.1);
					case "KILO_VERME" -> assertThat(perWeek).isLessThan(-0.1);
					default -> assertThat(Math.abs(perWeek)).isLessThanOrEqualTo(0.15);
				}
			});
		}

		@Test
		void shouldReportWaistChangePerKiloWhenTapeDataExists() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>();
			double weight = 78.0;
			double waist = 82.0;
			for (int week = 0; week < 40; week++) {
				measurements.add(weigh(MONDAY.plusWeeks(week), weight, waist));
				weight += 0.2;
				waist += 0.1;
			}

			List<Period> periods = BodyAnalyzer.analyze(study, measurements, List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).isNotEmpty();
			assertThat(periods.get(0).waistChangeCm()).isPositive();
			assertThat(periods.get(0).waistPerKg()).isNotNull().isPositive();
		}

		/**
		 * Someone who weighs in daily but reaches for the tape once a month ends nearly every
		 * period on a weight-only day. Reading the waist from the period's own last day would
		 * drop the figure for a record that plainly holds it.
		 */
		@Test
		void shouldReportWaistChangeWhenTheLastDaysCarryWeightOnly() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>();
			double weight = 86.0;
			double waist = 92.0;
			for (int week = 0; week < 40; week++) {
				measurements.add(weigh(MONDAY.plusWeeks(week), weight, week % 4 == 0 ? waist : null));
				weight -= 0.3;
				waist -= 0.2;
			}

			List<Period> periods = BodyAnalyzer.analyze(study, measurements, List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).isNotEmpty();
			assertThat(periods.get(0).waistChangeCm()).isNotNull().isNegative();
			assertThat(periods.get(0).waistPerKg()).isNotNull().isPositive();
		}

		/** One tape reading in the whole period says nothing about a change. */
		@Test
		void shouldReportNoWaistChangeWhenOnlyOneDayCarriesTheTape() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>();
			double weight = 86.0;
			for (int week = 0; week < 40; week++) {
				measurements.add(weigh(MONDAY.plusWeeks(week), weight, week == 10 ? 92.0 : null));
				weight -= 0.3;
			}

			List<Period> periods = BodyAnalyzer.analyze(study, measurements, List.of(), CURRENT_YEAR)
					.longTermPeriods();

			assertThat(periods).isNotEmpty();
			assertThat(periods.get(0).waistChangeCm()).isNull();
			assertThat(periods.get(0).waistPerKg()).isNull();
		}
	}

	@Nested
	class Composition {

		@Test
		void shouldUseNavyWhenNeckAndWaistExist() {
			study = studyWithBody(180, Sex.ERKEK, 1995);
			CaseStudyMeasurement latest = weigh(MONDAY, 80.0, 85.0);
			latest.setNeckCm(BigDecimal.valueOf(38.0));

			var composition = BodyAnalyzer.analyze(study, List.of(latest), List.of(), CURRENT_YEAR)
					.bodyComposition();

			assertThat(composition.method()).contains("Navy");
			assertThat(composition.bodyFatPercent()).isCloseTo(16.1, within(1.0));
			assertThat(composition.uncertaintyPoints()).isEqualTo(3.5);
			assertThat(composition.leanMassKg() + composition.fatMassKg()).isCloseTo(80.0, within(0.2));
			assertThat(composition.asOf()).isEqualTo(MONDAY);
		}

		@Test
		void shouldFallBackToBmiFormulaWithoutANeckMeasurement() {
			study = studyWithBody(180, Sex.ERKEK, 1995);

			var analysis = BodyAnalyzer.analyze(study, List.of(weigh(MONDAY, 80.0, 85.0)), List.of(),
					CURRENT_YEAR);

			assertThat(analysis.bodyComposition().method()).contains("Deurenberg");
			assertThat(analysis.bodyComposition().uncertaintyPoints()).isEqualTo(5.0);
			assertThat(analysis.bodyComposition().note()).contains("Boyun ölçüsü");
			assertThat(analysis.caveats()).anyMatch(c -> c.startsWith("Boyun ölçüsü yok"));
		}

		@Test
		void shouldRefuseToGuessWithoutHeightOrSex() {
			var composition = BodyAnalyzer.analyze(study, List.of(weigh(MONDAY, 80.0)), List.of(),
					CURRENT_YEAR).bodyComposition();

			assertThat(composition.bodyFatPercent()).isNull();
			assertThat(composition.method()).isNull();
			assertThat(composition.note()).contains("eksik veri").contains("boy");
		}

		/** The newest weigh-in often has no tape numbers; the newest complete one is used. */
		@Test
		void shouldReachBackToTheMostRecentRowWithTapeData() {
			study = studyWithBody(180, Sex.ERKEK, 1995);
			CaseStudyMeasurement complete = weigh(MONDAY, 80.0, 85.0);
			complete.setNeckCm(BigDecimal.valueOf(38.0));

			var composition = BodyAnalyzer.analyze(study,
					List.of(complete, weigh(MONDAY.plusDays(7), 79.5), weigh(MONDAY.plusDays(14), 79.0)),
					List.of(), CURRENT_YEAR).bodyComposition();

			assertThat(composition.method()).contains("Navy");
			assertThat(composition.asOf()).isEqualTo(MONDAY);
		}
	}

	@Nested
	class Robustness {

		@Test
		void shouldProduceTheSameResultFromUnorderedInput() {
			List<CaseStudyMeasurement> measurements = new ArrayList<>(twoYearsWeekly());
			List<CaseStudyIntake> intakes = new ArrayList<>(
					dailyIntake(MONDAY.plusWeeks(88), 120, 2400));

			var ordered = BodyAnalyzer.analyze(study, measurements, intakes, CURRENT_YEAR);
			Collections.shuffle(measurements, new java.util.Random(7));
			Collections.shuffle(intakes, new java.util.Random(11));
			var shuffled = BodyAnalyzer.analyze(study, measurements, intakes, CURRENT_YEAR);

			assertThat(shuffled.dailySeries()).isEqualTo(ordered.dailySeries());
			assertThat(shuffled.weeklyRows()).isEqualTo(ordered.weeklyRows());
			assertThat(shuffled.tdeeWindows()).isEqualTo(ordered.tdeeWindows());
			assertThat(shuffled.longTermPeriods()).isEqualTo(ordered.longTermPeriods());
		}

		@Test
		void shouldSurviveAnEmptyStudy() {
			var analysis = BodyAnalyzer.analyze(study, List.of(), List.of(), CURRENT_YEAR);

			assertThat(analysis.dailySeries()).isEmpty();
			assertThat(analysis.weeklyRows()).isEmpty();
			assertThat(analysis.tdeeWindows()).isEmpty();
			assertThat(analysis.calorieBuckets()).isEmpty();
			assertThat(analysis.longTermPeriods()).isEmpty();
			assertThat(analysis.coverage().measurementDays()).isZero();
			assertThat(analysis.bodyComposition().bodyFatPercent()).isNull();
			assertThat(analysis.caveats()).anyMatch(c -> c.startsWith("Kalori kaydı yok"));
		}

		@Test
		void shouldPairDailyCaloriesWithTheDayTheyWereLoggedFor() {
			var analysis = BodyAnalyzer.analyze(study, dailyWeighIns(MONDAY, 3, 80.0, -0.1),
					List.of(ate(MONDAY, 2100), ate(MONDAY.plusDays(2), 2900)), CURRENT_YEAR);

			assertThat(analysis.dailySeries()).hasSize(3);
			assertThat(analysis.dailySeries().get(0).calories()).isEqualTo(2100);
			assertThat(analysis.dailySeries().get(1).calories()).isNull();
			assertThat(analysis.dailySeries().get(2).calories()).isEqualTo(2900);
			assertThat(analysis.dailySeries().get(0).trendWeightKg()).isEqualTo(80.0);
		}
	}
}
