package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.service.AttentionScorer.Signals;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttentionScorerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
	private static final LocalDate LONG_AGO = TODAY.minusMonths(6);

	private static Signals base() {
		// Active for months, plans of both kinds, everything logged yesterday: nothing to flag.
		return new Signals(UUID.randomUUID(), "Ahmet", LONG_AGO, true, true, TODAY.minusDays(1),
				TODAY.minusDays(3), TODAY.minusDays(1), false, TODAY.plusMonths(2));
	}

	private static Signals with(java.util.function.UnaryOperator<Builder> change) {
		return change.apply(new Builder(base())).build();
	}

	/** A tiny wither so each test changes one thing. */
	private record Builder(Signals s) {
		Builder workout(LocalDate d) {
			return new Builder(new Signals(s.memberId(), s.fullName(), s.joinedOn(), s.hasTrainingPlan(),
					s.hasNutritionPlan(), d, s.lastMeasurement(), s.lastIntake(), s.unreadMessage(), s.membershipEnd()));
		}

		Builder measurement(LocalDate d) {
			return new Builder(new Signals(s.memberId(), s.fullName(), s.joinedOn(), s.hasTrainingPlan(),
					s.hasNutritionPlan(), s.lastWorkout(), d, s.lastIntake(), s.unreadMessage(), s.membershipEnd()));
		}

		Builder intake(LocalDate d) {
			return new Builder(new Signals(s.memberId(), s.fullName(), s.joinedOn(), s.hasTrainingPlan(),
					s.hasNutritionPlan(), s.lastWorkout(), s.lastMeasurement(), d, s.unreadMessage(), s.membershipEnd()));
		}

		Builder joined(LocalDate d) {
			return new Builder(new Signals(s.memberId(), s.fullName(), d, s.hasTrainingPlan(), s.hasNutritionPlan(),
					null, null, null, s.unreadMessage(), s.membershipEnd()));
		}

		Builder plans(boolean training, boolean nutrition) {
			return new Builder(new Signals(s.memberId(), s.fullName(), s.joinedOn(), training, nutrition,
					s.lastWorkout(), s.lastMeasurement(), s.lastIntake(), s.unreadMessage(), s.membershipEnd()));
		}

		Builder unread() {
			return new Builder(new Signals(s.memberId(), s.fullName(), s.joinedOn(), s.hasTrainingPlan(),
					s.hasNutritionPlan(), s.lastWorkout(), s.lastMeasurement(), s.lastIntake(), true, s.membershipEnd()));
		}

		Builder membershipEnd(LocalDate d) {
			return new Builder(new Signals(s.memberId(), s.fullName(), s.joinedOn(), s.hasTrainingPlan(),
					s.hasNutritionPlan(), s.lastWorkout(), s.lastMeasurement(), s.lastIntake(), s.unreadMessage(), d));
		}

		Signals build() {
			return s;
		}
	}

	private static java.util.List<String> codes(AttentionScorer.Item item) {
		return item.reasons().stream().map(AttentionScorer.Reason::code).toList();
	}

	@Test
	void shouldFlagNothingForAMemberWhoIsKeepingUp() {
		assertThat(AttentionScorer.score(base(), TODAY)).isNull();
	}

	@Test
	void shouldFlagAWorkoutGapAtSevenDaysAndRateItHigherAtFourteen() {
		var seven = AttentionScorer.score(with(b -> b.workout(TODAY.minusDays(7))), TODAY);
		var six = AttentionScorer.score(with(b -> b.workout(TODAY.minusDays(6))), TODAY);
		var fourteen = AttentionScorer.score(with(b -> b.workout(TODAY.minusDays(14))), TODAY);

		assertThat(six).isNull();
		assertThat(codes(seven)).containsExactly("NO_WORKOUT");
		assertThat(fourteen.score()).isGreaterThan(seven.score());
	}

	/** Nobody is chased for skipping training they were never given. */
	@Test
	void shouldNotCountWorkoutOrFoodSilenceWithoutAPlan() {
		var item = AttentionScorer.score(
				with(b -> b.plans(false, false).workout(TODAY.minusDays(30)).intake(TODAY.minusDays(30))), TODAY);

		assertThat(item).isNull();
	}

	@Test
	void shouldFlagAFoodLogGapFromThreeDays() {
		assertThat(AttentionScorer.score(with(b -> b.intake(TODAY.minusDays(2))), TODAY)).isNull();
		assertThat(codes(AttentionScorer.score(with(b -> b.intake(TODAY.minusDays(3))), TODAY)))
				.containsExactly("NO_INTAKE");
	}

	@Test
	void shouldFlagAMissingMeasurementFromTenDays() {
		assertThat(AttentionScorer.score(with(b -> b.measurement(TODAY.minusDays(9))), TODAY)).isNull();
		assertThat(codes(AttentionScorer.score(with(b -> b.measurement(TODAY.minusDays(10))), TODAY)))
				.containsExactly("NO_MEASUREMENT");
	}

	/** A member who joined yesterday has logged nothing, and that is not a problem. */
	@Test
	void shouldNotFlagANewMemberWhoHasNotHadTimeToLogAnything() {
		var item = AttentionScorer.score(with(b -> b.joined(TODAY.minusDays(1))), TODAY);

		assertThat(item).isNull();
	}

	/** One who joined a month ago and never logged is counted from the day they joined. */
	@Test
	void shouldCountSilenceFromTheJoinDateWhenNothingWasEverLogged() {
		var item = AttentionScorer.score(with(b -> b.joined(TODAY.minusDays(30))), TODAY);

		assertThat(codes(item)).contains("NO_WORKOUT", "NO_MEASUREMENT", "NO_INTAKE");
		assertThat(item.reasons().stream().filter(r -> r.code().equals("NO_WORKOUT")).findFirst().orElseThrow().label())
				.contains("30 gündür");
	}

	@Test
	void shouldFlagAnUnreadMessage() {
		assertThat(codes(AttentionScorer.score(with(Builder::unread), TODAY))).containsExactly("MESSAGE");
	}

	@Test
	void shouldFlagAMembershipThatEndsSoonOrHasEnded() {
		var soon = AttentionScorer.score(with(b -> b.membershipEnd(TODAY.plusDays(14))), TODAY);
		var later = AttentionScorer.score(with(b -> b.membershipEnd(TODAY.plusDays(15))), TODAY);
		var ended = AttentionScorer.score(with(b -> b.membershipEnd(TODAY.minusDays(3))), TODAY);

		assertThat(codes(soon)).containsExactly("MEMBERSHIP_ENDING");
		assertThat(later).isNull();
		assertThat(codes(ended)).containsExactly("MEMBERSHIP_ENDED");
		assertThat(ended.score()).isGreaterThan(soon.score());
	}

	@Test
	void shouldNotFlagAMemberWhoNeverHadAMembershipRecord() {
		// Members from before memberships existed have no end date; that is not "expired".
		assertThat(AttentionScorer.score(with(b -> b.membershipEnd(null)), TODAY)).isNull();
	}

	@Test
	void shouldAddUpReasonsAndListTheMostSevereFirst() {
		var item = AttentionScorer.score(with(b -> b.unread().workout(TODAY.minusDays(20))
				.membershipEnd(TODAY.minusDays(1))), TODAY);

		assertThat(item.score()).isEqualTo(4 + 4 + 5);
		assertThat(item.reasons().get(0).code()).isEqualTo("MEMBERSHIP_ENDED");
	}
}
