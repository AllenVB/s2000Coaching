package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.service.ReminderPlanner.Reminder;
import com.s2000coaching.service.ReminderPlanner.Signals;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderPlannerTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);
	private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 27);
	private static final LocalDate LONG_AGO = LocalDate.of(2026, 1, 5);
	private static final String URL = "https://s2000.example";

	private static Signals signals(boolean enabled, boolean consent, LocalDate joined, LocalDate lastMeasurement,
			LocalDate lastCheckIn, LocalDate membershipEnd) {
		// The monthly table is up to date here, so the older rules are tested on their own.
		return new Signals("Ahmet Yılmaz", enabled, consent, joined, lastMeasurement, lastCheckIn, membershipEnd, "2026-09");
	}

	private static List<String> kinds(List<Reminder> reminders) {
		return reminders.stream().map(Reminder::kind).toList();
	}

	@Test
	void shouldSendNothingToAMemberWhoSwitchedRemindersOff() {
		var plan = ReminderPlanner.plan(signals(false, true, LONG_AGO, LONG_AGO, LONG_AGO, MONDAY.plusDays(3)),
				MONDAY, URL);

		assertThat(plan).isEmpty();
	}

	@Test
	void shouldWarnAWeekAheadOfTheMembershipEnding() {
		var plan = ReminderPlanner.plan(
				signals(true, true, LONG_AGO, TUESDAY, TUESDAY, TUESDAY.plusDays(7)), TUESDAY, URL);

		assertThat(kinds(plan)).containsExactly("MEMBERSHIP_ENDING");
		assertThat(plan.get(0).subject()).contains("7 gün");
		assertThat(plan.get(0).occasion()).isEqualTo(TUESDAY.plusDays(7).toString());
		assertThat(plan.get(0).body()).startsWith("Merhaba Ahmet,");
	}

	@Test
	void shouldNotWarnEarlierThanTheNoticeWindow() {
		assertThat(ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY, TUESDAY, TUESDAY.plusDays(8)),
				TUESDAY, URL)).isEmpty();
	}

	@Test
	void shouldSendALastDayReminderAndThenAnEndedOne() {
		var lastDay = ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY, TUESDAY, TUESDAY.plusDays(1)),
				TUESDAY, URL);
		var ended = ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY, TUESDAY, TUESDAY.minusDays(2)),
				TUESDAY, URL);
		var longEnded = ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY, TUESDAY, TUESDAY.minusDays(30)),
				TUESDAY, URL);

		assertThat(kinds(lastDay)).containsExactly("MEMBERSHIP_LAST_DAY");
		assertThat(kinds(ended)).containsExactly("MEMBERSHIP_ENDED");
		assertThat(longEnded).isEmpty(); // a month later it is no longer a reminder, just silence
	}

	@Test
	void shouldAskForTheWeeklyCheckInOnMondaysWhenLastWeekIsMissing() {
		var plan = ReminderPlanner.plan(
				signals(true, true, LONG_AGO, MONDAY, LocalDate.of(2026, 10, 12), null), MONDAY, URL);

		assertThat(kinds(plan)).containsExactly("WEEKLY_CHECKIN");
		assertThat(plan.get(0).occasion()).isEqualTo("2026-10-19"); // the week that just ended
	}

	@Test
	void shouldNotAskForTheCheckInOnOtherDaysOrWhenItWasSent() {
		var notMonday = ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY, null, null), TUESDAY, URL);
		var sent = ReminderPlanner.plan(
				signals(true, true, LONG_AGO, MONDAY, LocalDate.of(2026, 10, 19), null), MONDAY, URL);

		assertThat(notMonday).isEmpty();
		assertThat(sent).isEmpty();
	}

	@Test
	void shouldNotNagANewMemberAboutTheCheckInTheyWereNeverAsked() {
		var plan = ReminderPlanner.plan(signals(true, true, MONDAY.minusDays(2), MONDAY, null, null), MONDAY, URL);

		assertThat(plan).isEmpty();
	}

	@Test
	void shouldRemindAboutAMeasurementOnceItIsTenDaysOld() {
		var nine = ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY.minusDays(9), TUESDAY, null), TUESDAY, URL);
		var ten = ReminderPlanner.plan(signals(true, true, LONG_AGO, TUESDAY.minusDays(10), TUESDAY, null), TUESDAY, URL);

		assertThat(nine).isEmpty();
		assertThat(kinds(ten)).containsExactly("MEASUREMENT");
		// The occasion is the measurement it lapsed from: the same lapse is one reminder, ever.
		assertThat(ten.get(0).occasion()).isEqualTo(TUESDAY.minusDays(10).toString());
	}

	/** Reminders about health records need the consent they cannot be recorded without. */
	@Test
	void shouldNotAskForCheckInsOrMeasurementsFromAMemberWhoCannotWriteThem() {
		var plan = ReminderPlanner.plan(signals(true, false, LONG_AGO, LONG_AGO, null, null), MONDAY, URL);

		assertThat(plan).isEmpty();
	}

	@Test
	void shouldMeasureTheQuietFromTheJoinDateWhenNothingWasEverMeasured() {
		var plan = ReminderPlanner.plan(signals(true, true, TUESDAY.minusDays(12), null, null, null), TUESDAY, URL);

		assertThat(kinds(plan)).containsExactly("MEASUREMENT");
		assertThat(plan.get(0).occasion()).isEqualTo("none");
	}

	@Test
	void shouldAskOnceForAMonthlyTableThatWasNotEntered() {
		var behind = new Signals("Ahmet Yılmaz", true, true, LONG_AGO, TUESDAY, TUESDAY, null, "2026-08");

		var plan = ReminderPlanner.plan(behind, TUESDAY, URL);

		assertThat(kinds(plan)).containsExactly("MONTHLY_RECORD");
		assertThat(plan.get(0).occasion()).isEqualTo("2026-09");
		assertThat(plan.get(0).body()).contains("/uye/olcumlerim?sekme=aylik");
	}

	@Test
	void shouldNotAskForAMonthlyTableWhenLastMonthIsInOrTheMemberIsNew() {
		var current = new Signals("Ahmet Yılmaz", true, true, LONG_AGO, TUESDAY, TUESDAY, null, "2026-09");
		var newcomer = new Signals("Ahmet Yılmaz", true, true, LocalDate.of(2026, 10, 20), TUESDAY, TUESDAY, null, null);

		assertThat(ReminderPlanner.plan(current, TUESDAY, URL)).isEmpty();
		assertThat(ReminderPlanner.plan(newcomer, TUESDAY, URL)).isEmpty();
	}

	@Test
	void shouldPointTheWeeklyReminderAtTheNewFollowUpPage() {
		var plan = ReminderPlanner.plan(signals(true, true, LONG_AGO, MONDAY, null, null), MONDAY, URL);

		assertThat(plan).anySatisfy(reminder -> assertThat(reminder.body()).contains("/uye/olcumlerim?sekme=haftalik"));
	}
}
