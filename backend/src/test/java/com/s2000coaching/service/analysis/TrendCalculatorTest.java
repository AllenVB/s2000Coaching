package com.s2000coaching.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.s2000coaching.service.analysis.TrendCalculator.Observation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TrendCalculatorTest {

	private static final LocalDate START = LocalDate.of(2025, 1, 1);

	@Nested
	class ExponentialTrend {

		/**
		 * The property the whole gap-scaling exists for: the same stretch of time has to be
		 * smoothed by the same amount whether it was weighed every day or once that week.
		 * Without it the two-year weekly stretch would be smoothed seven times less than the
		 * four daily months, and the two halves of the chart would not be comparable.
		 */
		@Test
		void shouldSmoothTheSameAmountForDailyAndWeeklyCadence() {
			List<Observation> daily = new ArrayList<>();
			for (int day = 0; day <= 28; day++) {
				daily.add(new Observation(START.plusDays(day), day == 0 ? 80 : 78));
			}
			List<Observation> weekly = List.of(
					new Observation(START, 80),
					new Observation(START.plusDays(7), 78),
					new Observation(START.plusDays(14), 78),
					new Observation(START.plusDays(21), 78),
					new Observation(START.plusDays(28), 78));

			List<Observation> dailyTrend = TrendCalculator.exponentialTrend(daily,
					TrendCalculator.DAILY_ALPHA);
			List<Observation> weeklyTrend = TrendCalculator.exponentialTrend(weekly,
					TrendCalculator.DAILY_ALPHA);

			for (Observation weeklyPoint : weeklyTrend) {
				double dailyValue = dailyTrend.stream()
						.filter(o -> o.date().equals(weeklyPoint.date()))
						.findFirst().orElseThrow().value();
				assertThat(weeklyPoint.value()).isCloseTo(dailyValue, within(1e-9));
			}
		}

		@Test
		void shouldStartAtTheFirstReading() {
			List<Observation> trend = TrendCalculator.exponentialTrend(
					List.of(new Observation(START, 81.4), new Observation(START.plusDays(1), 79)),
					TrendCalculator.DAILY_ALPHA);

			assertThat(trend.get(0).value()).isEqualTo(81.4);
		}

		/** Smoothing is the point: a single heavy day must not move the line much. */
		@Test
		void shouldDampASingleOutlier() {
			List<Observation> points = new ArrayList<>();
			for (int day = 0; day < 20; day++) {
				points.add(new Observation(START.plusDays(day), day == 10 ? 84.0 : 80.0));
			}

			List<Observation> trend = TrendCalculator.exponentialTrend(points,
					TrendCalculator.DAILY_ALPHA);

			assertThat(trend.get(10).value()).isLessThan(80.5);
			assertThat(trend.get(19).value()).isCloseTo(80.0, within(0.3));
		}

		@Test
		void shouldReturnEmptyForNoPoints() {
			assertThat(TrendCalculator.exponentialTrend(List.of(), TrendCalculator.DAILY_ALPHA)).isEmpty();
		}

		/** Two readings on one day would make the gap zero; the floor of one day keeps it sane. */
		@Test
		void shouldTolerateTwoReadingsOnTheSameDay() {
			List<Observation> trend = TrendCalculator.exponentialTrend(
					List.of(new Observation(START, 80), new Observation(START, 90)),
					TrendCalculator.DAILY_ALPHA);

			assertThat(trend).hasSize(2);
			assertThat(trend.get(1).value()).isCloseTo(81.0, within(1e-9));
		}
	}

	@Nested
	class Correlation {

		@Test
		void shouldRefuseFewerThanSixPairs() {
			assertThat(TrendCalculator.correlation(
					List.of(1.0, 2.0, 3.0, 4.0, 5.0), List.of(1.0, 2.0, 3.0, 4.0, 5.0))).isNull();
		}

		@Test
		void shouldReportPerfectPositiveRelationship() {
			assertThat(TrendCalculator.correlation(
					List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), List.of(2.0, 4.0, 6.0, 8.0, 10.0, 12.0)))
					.isCloseTo(1.0, within(1e-9));
		}

		@Test
		void shouldReportPerfectNegativeRelationship() {
			assertThat(TrendCalculator.correlation(
					List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), List.of(6.0, 5.0, 4.0, 3.0, 2.0, 1.0)))
					.isCloseTo(-1.0, within(1e-9));
		}

		@Test
		void shouldReturnNullWhenOneSideNeverVaries() {
			assertThat(TrendCalculator.correlation(
					List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), List.of(3.0, 3.0, 3.0, 3.0, 3.0, 3.0))).isNull();
		}

		@Test
		void shouldReturnNullForMismatchedLengths() {
			assertThat(TrendCalculator.correlation(
					List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0), List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)))
					.isNull();
		}
	}

	@Nested
	class Slope {

		@Test
		void shouldMeasureRiseOverRun() {
			assertThat(TrendCalculator.slope(List.of(0.0, 1.0, 2.0, 3.0), List.of(10.0, 12.0, 14.0, 16.0)))
					.isCloseTo(2.0, within(1e-9));
		}

		@Test
		void shouldReturnNullWhenXNeverVaries() {
			assertThat(TrendCalculator.slope(List.of(5.0, 5.0, 5.0), List.of(1.0, 2.0, 3.0))).isNull();
		}

		@Test
		void shouldReturnNullForASinglePoint() {
			assertThat(TrendCalculator.slope(List.of(1.0), List.of(1.0))).isNull();
		}
	}

	@Nested
	class Round {

		@Test
		void shouldRoundToGivenDecimals() {
			assertThat(TrendCalculator.round(2.34567, 2)).isEqualTo(2.35);
			assertThat(TrendCalculator.round(2529.6, 0)).isEqualTo(2530.0);
			assertThat(TrendCalculator.round(-0.456, 1)).isEqualTo(-0.5);
		}
	}
}
