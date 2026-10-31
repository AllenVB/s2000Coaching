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
		return new F().build();
	}

	private static Signals with(java.util.function.UnaryOperator<F> change) {
		return change.apply(new F()).build();
	}

	/** Mutable fields with a sensible "all is well" default, so each test changes one thing. */
	private static final class F {
		LocalDate joined = LONG_AGO;
		boolean trainingPlan = true;
		boolean nutritionPlan = true;
		LocalDate workout = TODAY.minusDays(1);
		LocalDate measurement = TODAY.minusDays(3);
		LocalDate intake = TODAY.minusDays(1);
		boolean unread;
		LocalDate membershipEnd = TODAY.plusMonths(2);
		boolean unansweredCheckIn;
		boolean questionnaire = true;
		boolean clearance;
		boolean formCheck;
		LocalDate dailyIntake = TODAY.minusDays(1);
		int overTargetDays;
		boolean stepsEver;
		Integer averageSteps;

		F dailyIntake(LocalDate d) {
			dailyIntake = d;
			return this;
		}

		F overTarget(int days) {
			overTargetDays = days;
			return this;
		}

		F steps(Integer average) {
			stepsEver = true;
			averageSteps = average;
			return this;
		}

		F workout(LocalDate d) {
			workout = d;
			return this;
		}

		F measurement(LocalDate d) {
			measurement = d;
			return this;
		}

		F intake(LocalDate d) {
			intake = d;
			return this;
		}

		/** Joined on d, with nothing ever logged. */
		F joined(LocalDate d) {
			joined = d;
			workout = null;
			measurement = null;
			intake = null;
			return this;
		}

		F plans(boolean training, boolean nutrition) {
			trainingPlan = training;
			nutritionPlan = nutrition;
			return this;
		}

		F unread() {
			unread = true;
			return this;
		}

		F membershipEnd(LocalDate d) {
			membershipEnd = d;
			return this;
		}

		F checkIn() {
			unansweredCheckIn = true;
			return this;
		}

		F noQuestionnaire() {
			questionnaire = false;
			return this;
		}

		F formCheck() {
			formCheck = true;
			return this;
		}

		F clearance() {
			clearance = true;
			return this;
		}

		Signals build() {
			return new Signals(UUID.randomUUID(), "Ahmet", joined, trainingPlan, nutritionPlan, workout, measurement,
					intake, unread, membershipEnd, unansweredCheckIn, questionnaire, clearance, formCheck, dailyIntake,
					overTargetDays, stepsEver, averageSteps);
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
	void shouldFlagAWeeklyFoodGapFromTenDaysAfterTheLastEnteredWeekEnded() {
		assertThat(AttentionScorer.score(with(b -> b.intake(TODAY.minusDays(9))), TODAY)).isNull();
		assertThat(codes(AttentionScorer.score(with(b -> b.intake(TODAY.minusDays(10))), TODAY)))
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
	void shouldFlagACheckInTheCoachHasNotAnsweredYet() {
		assertThat(codes(AttentionScorer.score(with(F::checkIn), TODAY))).containsExactly("CHECK_IN");
	}

	@Test
	void shouldFlagAnUnreadMessage() {
		assertThat(codes(AttentionScorer.score(with(F::unread), TODAY))).containsExactly("MESSAGE");
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

	@Test
	void shouldFlagAnAnswerThatNeedsADoctorsClearanceAboveEverythingQuiet() {
		var item = AttentionScorer.score(with(F::clearance), TODAY);

		assertThat(codes(item)).containsExactly("MEDICAL_CLEARANCE");
		assertThat(item.reasons().get(0).severity()).isEqualTo(4);
	}

	@Test
	void shouldGiveANewMemberAFewDaysToFillInTheQuestionnaire() {
		var fresh = AttentionScorer.score(with(b -> b.joined(TODAY.minusDays(2)).noQuestionnaire()), TODAY);
		var overdue = AttentionScorer.score(with(b -> b.joined(TODAY.minusDays(3)).noQuestionnaire()), TODAY);

		assertThat(fresh).isNull();
		assertThat(codes(overdue)).contains("NO_QUESTIONNAIRE");
	}

	@Test
	void shouldNotAskAgainOnceAnsweredEvenWhenTheAnswersNeedClearance() {
		var item = AttentionScorer.score(with(b -> b.noQuestionnaire().clearance()), TODAY);

		// Cannot both be true in practice; if it ever is, the clearance flag wins and no nag is added.
		assertThat(codes(item)).doesNotContain("NO_QUESTIONNAIRE");
	}

	@Test
	void shouldFlagAFormCheckVideoWaitingForAnAnswer() {
		assertThat(codes(AttentionScorer.score(with(F::formCheck), TODAY))).containsExactly("FORM_CHECK");
	}

	@Test
	void shouldFlagAMemberWhoStoppedTheDailyFoodLog() {
		var item = AttentionScorer.score(with(b -> b.intake(TODAY.minusDays(1)).dailyIntake(TODAY.minusDays(5))), TODAY);

		assertThat(codes(item)).containsExactly("NO_DAILY_INTAKE");
	}

	@Test
	void shouldNotChaseSomeoneWhoNeverStartedTheDailyLog() {
		assertThat(AttentionScorer.score(with(b -> b.dailyIntake(null)), TODAY)).isNull();
	}

	@Test
	void shouldFlagThreeDaysOverTargetButNotTwo() {
		assertThat(codes(AttentionScorer.score(with(b -> b.overTarget(3)), TODAY))).containsExactly("OVER_TARGET");
		assertThat(AttentionScorer.score(with(b -> b.overTarget(2)), TODAY)).isNull();
	}

	@Test
	void shouldFlagLowOrMissingStepsOnlyForMembersWhoLogThem() {
		assertThat(codes(AttentionScorer.score(with(b -> b.steps(3200)), TODAY))).containsExactly("LOW_STEPS");
		assertThat(codes(AttentionScorer.score(with(b -> b.steps(null)), TODAY))).containsExactly("NO_STEPS");
		assertThat(AttentionScorer.score(with(b -> b.steps(8000)), TODAY)).isNull();
		assertThat(AttentionScorer.score(base(), TODAY)).isNull();
	}
}
