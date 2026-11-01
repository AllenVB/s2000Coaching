package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklySummaryTest {

	@Test
	void shouldFormatLiraWithThousandsAndKurus() {
		assertThat(WeeklySummary.formatLira(1234590)).isEqualTo("12.345,90 TL");
		assertThat(WeeklySummary.formatLira(5)).isEqualTo("0,05 TL");
		assertThat(WeeklySummary.formatLira(0)).isEqualTo("0,00 TL");
	}

	@Test
	void shouldListTheNumbersAndSayHowManyMoreNeedAttention() {
		var text = WeeklySummary.build(new WeeklySummary.Numbers(40, 3, 120, 210, 28, 4, 2, 5, 3745000,
				List.of("Ahmet: 9 gündür antrenman kaydı yok"), 12, "https://s2000.example"));

		assertThat(text).contains("Aktif üye: 40", "Yeni katılan: 3", "Antrenman kaydı: 120", "Haftalık takip gönderen: 28 / 40",
				"Ciro: 37.450,00 TL", "dönüş bekleyen toplam: 2", "Ahmet: 9 gündür", "ve 11 üye daha",
				"https://s2000.example/yonetim");
	}

	@Test
	void shouldNotMentionAttentionWhenNobodyNeedsIt() {
		var text = WeeklySummary.build(new WeeklySummary.Numbers(1, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), 0, "https://x"));

		assertThat(text).doesNotContain("ilgi bekleyenler");
	}
}
