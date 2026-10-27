package com.s2000coaching.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides which reminders a member is due today. Pure, so every rule is testable without a
 * clock or a mail server.
 *
 * <p>Each reminder carries an {@code occasion}: the thing it is about (the membership end date,
 * the week, the last measurement). The same kind for the same occasion is sent once, ever; a new
 * occasion is a new reminder. That is what stops a member being mailed daily about the same
 * overdue measurement, while still reminding them again after they log one and lapse again.
 */
public final class ReminderPlanner {

	static final int MEASUREMENT_QUIET_DAYS = 10;
	static final int MEMBERSHIP_NOTICE_DAYS = 7;

	private ReminderPlanner() {
	}

	public record Signals(String fullName, boolean remindersEnabled, boolean hasHealthConsent, LocalDate joinedOn,
			LocalDate lastMeasurement, LocalDate lastCheckInWeek, LocalDate membershipEnd) {
	}

	public record Reminder(String kind, String occasion, String subject, String body) {
	}

	public static List<Reminder> plan(Signals s, LocalDate today, String siteUrl) {
		List<Reminder> due = new ArrayList<>();
		if (!s.remindersEnabled()) {
			return due;
		}
		String greeting = "Merhaba " + firstName(s.fullName()) + ",\n\n";
		String signature = "\n\nBu e-postaları Profilim sayfasından kapatabilirsiniz.\nS2000 Coaching\n" + siteUrl;

		if (s.membershipEnd() != null) {
			long left = ChronoUnit.DAYS.between(today, s.membershipEnd());
			String occasion = s.membershipEnd().toString();
			if (left > 1 && left <= MEMBERSHIP_NOTICE_DAYS) {
				due.add(new Reminder("MEMBERSHIP_ENDING", occasion, "Üyeliğiniz %d gün sonra bitiyor".formatted(left),
						greeting + "Üyelik süreniz %s tarihinde sona eriyor. Kesintisiz devam etmek için koçunuzla "
								.formatted(s.membershipEnd()) + "iletişime geçip yenileyebilirsiniz. Yenilemeyi erken yaparsanız kalan "
								+ "günleriniz kaybolmaz; yeni süre bitişin üstüne eklenir." + signature));
			} else if (left >= 0 && left <= 1) {
				due.add(new Reminder("MEMBERSHIP_LAST_DAY", occasion,
						left == 0 ? "Üyeliğiniz bugün bitiyor" : "Üyeliğiniz yarın bitiyor",
						greeting + "Üyelik süreniz %s tarihinde sona eriyor. Devam etmek için koçunuza yazın."
								.formatted(s.membershipEnd()) + signature));
			} else if (left < 0 && left >= -MEMBERSHIP_NOTICE_DAYS) {
				due.add(new Reminder("MEMBERSHIP_ENDED", occasion, "Üyelik süreniz doldu",
						greeting + "Üyelik süreniz %s tarihinde sona erdi. Birlikte devam etmek isterseniz koçunuza "
								.formatted(s.membershipEnd()) + "yazmanız yeterli." + signature));
			}
		}

		// Weekly check-in: on Mondays, for the week that just ended, if it was not sent.
		if (s.hasHealthConsent() && today.getDayOfWeek() == DayOfWeek.MONDAY) {
			LocalDate lastWeek = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			boolean joinedBefore = s.joinedOn() != null && s.joinedOn().isBefore(today.minusDays(3));
			boolean missing = s.lastCheckInWeek() == null || s.lastCheckInWeek().isBefore(lastWeek);
			if (joinedBefore && missing) {
				due.add(new Reminder("WEEKLY_CHECKIN", lastWeek.toString(), "Haftalık değerlendirmenizi gönderin",
						greeting + "Geçen haftanın değerlendirmesini henüz göndermediniz. Bir dakikanızı alır; "
								+ "koçunuz programınızı bu bilgilere göre ayarlıyor: " + siteUrl + "/uye/haftalik" + signature));
			}
		}

		if (s.hasHealthConsent()) {
			LocalDate from = s.lastMeasurement() == null || (s.joinedOn() != null && s.lastMeasurement().isBefore(s.joinedOn()))
					? s.joinedOn()
					: s.lastMeasurement();
			if (from != null && ChronoUnit.DAYS.between(from, today) >= MEASUREMENT_QUIET_DAYS) {
				due.add(new Reminder("MEASUREMENT", s.lastMeasurement() == null ? "none" : s.lastMeasurement().toString(),
						"Yeni ölçümünüzü girin",
						greeting + "Son ölçümünüzün üzerinden %d günden fazla geçti. Gelişiminizin izlenebilmesi için "
								.formatted(MEASUREMENT_QUIET_DAYS) + "kilonuzu girin: " + siteUrl + "/uye/olcumler" + signature));
			}
		}
		return due;
	}

	private static String firstName(String fullName) {
		String trimmed = fullName == null ? "" : fullName.trim();
		int space = trimmed.indexOf(' ');
		return space > 0 ? trimmed.substring(0, space) : trimmed;
	}
}
