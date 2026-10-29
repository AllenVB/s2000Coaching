package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.service.TrainingStatsCalculator.SetFact;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrainingStatsCalculatorTest {

	private static final UUID SQUAT = UUID.randomUUID();
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27); // a Tuesday

	private static SetFact set(LocalDate date, int reps, String kg, String rpe, String prescribed) {
		return new SetFact(SQUAT, "Squat", prescribed, date, reps, kg == null ? null : new BigDecimal(kg),
				rpe == null ? null : new BigDecimal(rpe));
	}

	private static TrainingStatsCalculator.ExerciseStats only(List<SetFact> facts) {
		return TrainingStatsCalculator.calculate(facts, TODAY).exercises().get(0);
	}

	@Test
	void shouldEstimateOneRepMaxWithEpley() {
		// 100 kg x 5 -> 100 * (1 + 5/30) = 116.7
		var best = TrainingStatsCalculator.estimate(set(TODAY, 5, "100", null, "5"));

		assertThat(best.estimatedOneRepMaxKg()).isEqualByComparingTo("116.7");
	}

	@Test
	void shouldTakeASingleAtItsOwnWeight() {
		assertThat(TrainingStatsCalculator.estimate(set(TODAY, 1, "140", null, "1")).estimatedOneRepMaxKg())
				.isEqualByComparingTo("140.0");
	}

	/** Past about twelve reps the formula is guesswork, so it gives no number rather than a bad one. */
	@Test
	void shouldNotEstimateFromHighRepSetsOrSetsWithoutWeight() {
		assertThat(TrainingStatsCalculator.estimate(set(TODAY, 13, "40", null, "12"))).isNull();
		assertThat(TrainingStatsCalculator.estimate(set(TODAY, 8, null, null, "8"))).isNull();
		assertThat(TrainingStatsCalculator.estimate(set(TODAY, 8, "0", null, "8"))).isNull();
	}

	@Test
	void shouldFlagARecordOnlyWhenTheLastSessionBeatsEarlierOnes() {
		var facts = List.of(
				set(TODAY.minusDays(14), 8, "80", null, "8-10"),
				set(TODAY.minusDays(7), 8, "82.5", null, "8-10"),
				set(TODAY, 8, "85", null, "8-10"));

		assertThat(only(facts).newRecordInLastSession()).isTrue();
		assertThat(only(facts).best().weightKg()).isEqualByComparingTo("85");
	}

	@Test
	void shouldNotCallTheVeryFirstSessionARecord() {
		assertThat(only(List.of(set(TODAY, 8, "80", null, "8-10"))).newRecordInLastSession()).isFalse();
	}

	@Test
	void shouldNotFlagARecordWhenTheLastSessionWasWorse() {
		var facts = List.of(set(TODAY.minusDays(7), 8, "90", null, "8-10"), set(TODAY, 8, "80", null, "8-10"));

		assertThat(only(facts).newRecordInLastSession()).isFalse();
	}

	@Test
	void shouldSuggestMoreWeightWhenEverySetHitTheTopOfTheRange() {
		var facts = List.of(set(TODAY.minusDays(7), 10, "80", "8", "8-10"), set(TODAY.minusDays(7), 10, "80", "8", "8-10"),
				set(TODAY, 10, "80", "8", "8-10"), set(TODAY, 10, "80", "8", "8-10"));

		var suggestion = only(facts).suggestion();

		assertThat(suggestion.kind()).isEqualTo("INCREASE");
		assertThat(suggestion.nextWeightKg()).isEqualByComparingTo("82.5");
	}

	@Test
	void shouldStepByFiveKilosOnceThebarIsAtOneHundred() {
		var facts = List.of(set(TODAY.minusDays(7), 5, "100", null, "5"), set(TODAY, 5, "100", null, "5"));

		assertThat(only(facts).suggestion().nextWeightKg()).isEqualByComparingTo("105");
	}

	@Test
	void shouldHoldWhenTheTopOfTheRangeWasReachedButFelt_Hard() {
		var facts = List.of(set(TODAY.minusDays(7), 10, "80", "9.5", "8-10"), set(TODAY, 10, "80", "9.5", "8-10"));

		var suggestion = only(facts).suggestion();

		assertThat(suggestion.kind()).isEqualTo("HOLD");
		assertThat(suggestion.text()).contains("RPE");
	}

	@Test
	void shouldHoldAndAskForMoreRepsWhenBelowTheRange() {
		var facts = List.of(set(TODAY.minusDays(7), 6, "80", null, "8-10"), set(TODAY, 6, "80", null, "8-10"));

		var suggestion = only(facts).suggestion();

		assertThat(suggestion.kind()).isEqualTo("HOLD");
		assertThat(suggestion.nextWeightKg()).isEqualByComparingTo("80");
		assertThat(suggestion.text()).contains("alt hedefin (8)");
	}

	@Test
	void shouldHoldInsideTheRange() {
		var facts = List.of(set(TODAY.minusDays(7), 9, "80", null, "8-10"), set(TODAY, 9, "80", null, "8-10"));

		assertThat(only(facts).suggestion().kind()).isEqualTo("HOLD");
	}

	/** An answer resting on one session must say so. */
	@Test
	void shouldSayWhenASuggestionRestsOnASingleSession() {
		var suggestion = only(List.of(set(TODAY, 10, "80", null, "8-10"))).suggestion();

		assertThat(suggestion.text()).contains("yalnızca 1 seansa dayanıyor");
	}

	@Test
	void shouldGiveNoSuggestionForANonNumericTarget() {
		var suggestion = only(List.of(set(TODAY, 20, "40", null, "AMRAP"))).suggestion();

		assertThat(suggestion.kind()).isEqualTo("INSUFFICIENT");
		assertThat(suggestion.nextWeightKg()).isNull();
	}

	@Test
	void shouldGiveNoSuggestionWhenNoWeightWasLogged() {
		var suggestion = only(List.of(set(TODAY, 12, null, null, "10-12"))).suggestion();

		assertThat(suggestion.kind()).isEqualTo("INSUFFICIENT");
	}

	@Test
	void shouldOnlyJudgeTheHeaviestWeightOfTheSessionNotWarmUps() {
		// Warm-up 40 x 5, then the working sets: the 40 must not drag the verdict down.
		var facts = new ArrayList<SetFact>();
		facts.add(set(TODAY, 5, "40", null, "8-10"));
		facts.add(set(TODAY, 10, "80", null, "8-10"));
		facts.add(set(TODAY, 10, "80", null, "8-10"));

		assertThat(only(facts).suggestion().kind()).isEqualTo("INCREASE");
	}

	@Test
	void shouldParseRepTargets() {
		assertThat(TrainingStatsCalculator.parseRange("8-10")).containsExactly(8, 10);
		assertThat(TrainingStatsCalculator.parseRange(" 12 ")).containsExactly(12, 12);
		assertThat(TrainingStatsCalculator.parseRange("8–10")).containsExactly(8, 10);
		assertThat(TrainingStatsCalculator.parseRange("AMRAP")).isNull();
		assertThat(TrainingStatsCalculator.parseRange("10-8")).isNull();
		assertThat(TrainingStatsCalculator.parseRange(null)).isNull();
	}

	@Test
	void shouldSumVolumePerWeekForTwelveWeeksIncludingEmptyOnes() {
		var facts = List.of(set(TODAY, 10, "80", null, "10"), set(TODAY.minusDays(1), 10, "80", null, "10"),
				set(TODAY.minusWeeks(2), 5, "100", null, "5"));

		var weeks = TrainingStatsCalculator.calculate(facts, TODAY).weeklyVolume();

		assertThat(weeks).hasSize(12);
		assertThat(weeks.get(11).weekStart()).isEqualTo(LocalDate.of(2026, 10, 26)); // Monday of this week
		assertThat(weeks.get(11).volumeKg()).isEqualByComparingTo("1600");
		assertThat(weeks.get(11).sets()).isEqualTo(2);
		assertThat(weeks.get(9).volumeKg()).isEqualByComparingTo("500");
		assertThat(weeks.get(10).sets()).isZero();
	}

	@Test
	void shouldIgnoreSessionsOlderThanTheVolumeWindow() {
		var weeks = TrainingStatsCalculator.calculate(List.of(set(TODAY.minusWeeks(30), 10, "80", null, "10")), TODAY)
				.weeklyVolume();

		assertThat(weeks).allSatisfy(week -> assertThat(week.sets()).isZero());
	}
}
