package com.s2000coaching.service;

import java.util.List;

/** The coach's Monday e-mail, as plain text. Pure: it formats the numbers it is given. */
public final class WeeklySummary {

	private WeeklySummary() {
	}

	/** Counts are for the last seven days unless stated. */
	public record Numbers(int activeMembers, long newMembers, long workoutsLogged, long foodDaysLogged,
			long checkInsSent, long newLeads, long leadsWaiting, long paidOrders, long revenueMinor,
			List<String> attention, int attentionTotal, String siteUrl) {
	}

	public static String build(Numbers n) {
		StringBuilder body = new StringBuilder("Günaydın,\n\nGeçen haftanın özeti:\n\n");
		body.append("Üyeler\n");
		body.append("  - Aktif üye: ").append(n.activeMembers()).append('\n');
		body.append("  - Yeni katılan: ").append(n.newMembers()).append('\n');
		body.append("\nÜyelerin girdiği kayıtlar\n");
		body.append("  - Antrenman kaydı: ").append(n.workoutsLogged()).append('\n');
		body.append("  - Günlük yemek kaydı: ").append(n.foodDaysLogged()).append(" gün\n");
		body.append("  - Haftalık takip gönderen: ").append(n.checkInsSent()).append(" / ").append(n.activeMembers()).append('\n');
		body.append("\nSatış\n");
		body.append("  - Ödenen sipariş: ").append(n.paidOrders()).append('\n');
		body.append("  - Ciro: ").append(formatLira(n.revenueMinor())).append('\n');
		body.append("  - Yeni ön değerlendirme başvurusu: ").append(n.newLeads())
				.append(" (dönüş bekleyen toplam: ").append(n.leadsWaiting()).append(")\n");
		if (!n.attention().isEmpty()) {
			body.append("\nBu hafta ilgi bekleyenler (en acilden)\n");
			n.attention().forEach(line -> body.append("  - ").append(line).append('\n'));
			if (n.attentionTotal() > n.attention().size()) {
				body.append("  ... ve ").append(n.attentionTotal() - n.attention().size()).append(" üye daha\n");
			}
		}
		body.append("\nPanel: ").append(n.siteUrl()).append("/yonetim\n");
		return body.toString();
	}

	/** 1234590 minor units -> "12.345,90 TL". */
	static String formatLira(long minor) {
		long lira = minor / 100;
		long kurus = minor % 100;
		String grouped = String.format(java.util.Locale.ROOT, "%,d", lira).replace(',', '.');
		return grouped + "," + String.format("%02d", kurus) + " TL";
	}
}
