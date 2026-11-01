package com.s2000coaching.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
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

	/** {@code lastMonthlyPeriod} is the newest month ("2026-09") the member filled in a table for, or null. */
	public record Signals(String fullName, boolean remindersEnabled, boolean hasHealthConsent, LocalDate joinedOn,
			LocalDate lastMeasurement, LocalDate lastCheckInWeek, LocalDate membershipEnd, String lastMonthlyPeriod) {
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
								.formatted(s.membershipEnd()) + "iletişime geçip ya da şuradan yenileyebilirsiniz: " + siteUrl + "/#paketler\nYenilemeyi erken yaparsanız kalan "
								+ "günleriniz kaybolmaz; yeni süre bitişin üstüne eklenir." + signature));
			} else if (left >= 0 && left <= 1) {
				due.add(new Reminder("MEMBERSHIP_LAST_DAY", occasion,
						left == 0 ? "Üyeliğiniz bugün bitiyor" : "Üyeliğiniz yarın bitiyor",
						greeting + "Üyelik süreniz %s tarihinde sona eriyor. Devam etmek için koçunuza yazın ya da paketinizi yenileyin: "
								.formatted(s.membershipEnd()) + siteUrl + "/#paketler" + signature));
			} else if (left < 0 && left >= -MEMBERSHIP_NOTICE_DAYS) {
				due.add(new Reminder("MEMBERSHIP_ENDED", occasion, "Üyelik süreniz doldu",
						greeting + "Üyelik süreniz %s tarihinde sona erdi. Birlikte devam etmek isterseniz koçunuza "
								.formatted(s.membershipEnd()) + "yazmanız ya da şuradan yenilemeniz yeterli: " + siteUrl + "/#paketler" + signature));
			}
		}

		// Weekly check-in: on Mondays, for the week that just ended, if it was not sent.
		if (s.hasHealthConsent() && today.getDayOfWeek() == DayOfWeek.MONDAY) {
			LocalDate lastWeek = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			boolean joinedBefore = s.joinedOn() != null && s.joinedOn().isBefore(today.minusDays(3));
			boolean missing = s.lastCheckInWeek() == null || s.lastCheckInWeek().isBefore(lastWeek);
			if (joinedBefore && missing) {
				due.add(new Reminder("WEEKLY_CHECKIN", lastWeek.toString(), "Haftalık takibinizi gönderin",
						greeting + "Geçen haftanın takibini (kilonuz, haftanın nasıl geçtiği, istek ve şikayetleriniz) "
								+ "henüz göndermediniz. Bir dakikanızı alır; koçunuz programınızı bu bilgilere göre "
								+ "ayarlıyor: " + siteUrl + "/uye/olcumlerim?sekme=haftalik" + signature));
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
								.formatted(MEASUREMENT_QUIET_DAYS) + "kilonuzu girin: " + siteUrl + "/uye/olcumlerim?sekme=haftalik" + signature));
			}
		}

		// Monthly tables: once a month has passed without one, ask for it once.
		if (s.hasHealthConsent() && s.joinedOn() != null && s.joinedOn().isBefore(today.withDayOfMonth(1))) {
			String previous = YearMonth.from(today).minusMonths(1).toString();
			boolean missing = s.lastMonthlyPeriod() == null || s.lastMonthlyPeriod().compareTo(previous) < 0;
			if (missing) {
				due.add(new Reminder("MONTHLY_RECORD", previous, "Aylık ölçülerinizi girin",
						greeting + "Geçen ayın ölçü tablosunu (boy, kilo ve çevre ölçüleriniz) henüz girmediniz. "
								+ "Kaydedilen veriler sonradan değiştirilemediği için sakin bir anda, doğru ölçerek girin: "
								+ siteUrl + "/uye/olcumlerim?sekme=aylik" + signature));
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
