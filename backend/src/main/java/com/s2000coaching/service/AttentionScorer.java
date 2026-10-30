package com.s2000coaching.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Decides who the coach should look at, and why. Pure: it is handed what is known about a
 * member and today's date, so every threshold can be tested without a database.
 *
 * <p>A silence is measured from the later of the last record and the day the member joined. A
 * member who signed up yesterday has logged nothing and that is not a problem; one who joined
 * a month ago and logged nothing is, and the days are counted from the day they joined.
 *
 * <p>Training silence only counts when the member has a plan to follow, and food-log silence
 * only when they have a nutrition plan — nobody is chased for skipping something they were
 * never given.
 */
public final class AttentionScorer {

	static final int WORKOUT_QUIET_DAYS = 7;
	static final int WORKOUT_VERY_QUIET_DAYS = 14;
	static final int MEASUREMENT_QUIET_DAYS = 10;
	static final int MEASUREMENT_VERY_QUIET_DAYS = 21;
	/**
	 * Food is entered once a week, after the week is over, so "quiet" is counted from the Sunday that
	 * closed the last entered week: a week is late three days after the next Sunday has passed.
	 */
	static final int INTAKE_QUIET_DAYS = 10;
	static final int INTAKE_VERY_QUIET_DAYS = 17;
	static final int MEMBERSHIP_ENDING_DAYS = 14;
	/** A new member is given a few days to fill in the starting questionnaire before being chased. */
	static final int QUESTIONNAIRE_GRACE_DAYS = 3;

	private AttentionScorer() {
	}

	/** What is known about one member. Dates are null when nothing has ever been recorded. */
	public record Signals(UUID memberId, String fullName, LocalDate joinedOn, boolean hasTrainingPlan,
			boolean hasNutritionPlan, LocalDate lastWorkout, LocalDate lastMeasurement, LocalDate lastIntake,
			boolean unreadMessage, LocalDate membershipEnd, boolean unansweredCheckIn, boolean hasQuestionnaire,
			boolean needsMedicalClearance, boolean unansweredFormCheck) {
	}

	/** {@code code} is stable for the UI; {@code label} is what the coach reads. */
	public record Reason(String code, String label, int severity) {
	}

	public record Item(UUID memberId, String fullName, int score, List<Reason> reasons) {
	}

	/** Null when there is nothing to flag. */
	public static Item score(Signals s, LocalDate today) {
		List<Reason> reasons = new ArrayList<>();

		if (s.unreadMessage()) {
			reasons.add(new Reason("MESSAGE", "Cevap bekleyen mesajı var", 4));
		}

		if (s.needsMedicalClearance()) {
			reasons.add(new Reason("MEDICAL_CLEARANCE", "Sağlık anketinde dikkat gerektiren yanıt var (doktor onayı)", 4));
		} else if (!s.hasQuestionnaire() && quietDays(null, s.joinedOn(), today) >= QUESTIONNAIRE_GRACE_DAYS) {
			reasons.add(new Reason("NO_QUESTIONNAIRE", "Sağlık anketini henüz doldurmadı", 2));
		}

		if (s.unansweredFormCheck()) {
			reasons.add(new Reason("FORM_CHECK", "Form kontrolü videosu yanıt bekliyor", 3));
		}

		if (s.unansweredCheckIn()) {
			reasons.add(new Reason("CHECK_IN", "Yanıt bekleyen haftalık değerlendirmesi var", 3));
		}

		if (s.membershipEnd() != null) {
			long left = ChronoUnit.DAYS.between(today, s.membershipEnd());
			if (left < 0) {
				reasons.add(new Reason("MEMBERSHIP_ENDED", "Üyelik süresi %d gün önce bitti".formatted(-left), 5));
			} else if (left <= MEMBERSHIP_ENDING_DAYS) {
				reasons.add(new Reason("MEMBERSHIP_ENDING",
						left == 0 ? "Üyelik bugün bitiyor" : "Üyelik %d gün sonra bitiyor".formatted(left), 3));
			}
		}

		if (s.hasTrainingPlan()) {
			long quiet = quietDays(s.lastWorkout(), s.joinedOn(), today);
			if (quiet >= WORKOUT_QUIET_DAYS) {
				reasons.add(new Reason("NO_WORKOUT", "%d gündür antrenman kaydı yok".formatted(quiet),
						quiet >= WORKOUT_VERY_QUIET_DAYS ? 4 : 2));
			}
		}

		long measurementQuiet = quietDays(s.lastMeasurement(), s.joinedOn(), today);
		if (measurementQuiet >= MEASUREMENT_QUIET_DAYS) {
			reasons.add(new Reason("NO_MEASUREMENT", "%d gündür ölçüm girmedi".formatted(measurementQuiet),
					measurementQuiet >= MEASUREMENT_VERY_QUIET_DAYS ? 3 : 2));
		}

		if (s.hasNutritionPlan()) {
			long quiet = quietDays(s.lastIntake(), s.joinedOn(), today);
			if (quiet >= INTAKE_QUIET_DAYS) {
				reasons.add(new Reason("NO_INTAKE", "%d gündür haftalık beslenme kaydı yok".formatted(quiet),
						quiet >= INTAKE_VERY_QUIET_DAYS ? 3 : 2));
			}
		}

		if (reasons.isEmpty()) {
			return null;
		}
		int score = reasons.stream().mapToInt(Reason::severity).sum();
		return new Item(s.memberId(), s.fullName(), score,
				reasons.stream().sorted((a, b) -> Integer.compare(b.severity(), a.severity())).toList());
	}

	private static long quietDays(LocalDate last, LocalDate joinedOn, LocalDate today) {
		LocalDate from = last == null || (joinedOn != null && last.isBefore(joinedOn)) ? joinedOn : last;
		return from == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(from, today));
	}
}
