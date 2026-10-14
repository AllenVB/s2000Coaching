package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CsvColumnMatcherTest {

	@Nested
	class Measurements {

		@Test
		void shouldMatchTurkishHeaders() {
			var mapping = CsvColumnMatcher.guessMeasurementColumns(
					List.of("Tarih", "Kilo", "Boyun", "Bel", "Göğüs", "Kalça", "Kol", "Bacak"));

			assertThat(mapping)
					.containsEntry("measuredOn", 0)
					.containsEntry("weightKg", 1)
					.containsEntry("neckCm", 2)
					.containsEntry("waistCm", 3)
					.containsEntry("chestCm", 4)
					.containsEntry("hipCm", 5)
					.containsEntry("armCm", 6)
					.containsEntry("thighCm", 7);
		}

		@Test
		void shouldMatchEnglishHeaders() {
			var mapping = CsvColumnMatcher.guessMeasurementColumns(
					List.of("Date", "Weight", "Neck", "Waist", "Chest", "Hips", "Arm", "Thigh"));

			assertThat(mapping)
					.containsEntry("measuredOn", 0)
					.containsEntry("weightKg", 1)
					.containsEntry("hipCm", 5);
		}

		@Test
		void shouldMatchHeadersCarryingTheirUnit() {
			var mapping = CsvColumnMatcher.guessMeasurementColumns(
					List.of("Tarih", "Kilo (kg)", "Bel (cm)"));

			assertThat(mapping).containsEntry("weightKg", 1).containsEntry("waistCm", 2);
		}

		@Test
		void shouldLeaveUnmatchedFieldsOut() {
			var mapping = CsvColumnMatcher.guessMeasurementColumns(List.of("Tarih", "Kilo"));

			assertThat(mapping).containsOnlyKeys("measuredOn", "weightKg");
		}

		/**
		 * The failure worth guarding: "yag" (fat) prefix-matching "yagorani" (fat percentage)
		 * and claiming a column that is not fat at all. Exact matches are resolved first, so a
		 * loose match can only take what nothing else claimed.
		 */
		@Test
		void shouldNotLetALooseMatchStealAColumnAnotherFieldMatchesExactly() {
			var mapping = CsvColumnMatcher.guessIntakeColumns(
					List.of("Tarih", "Yağ Oranı", "Yağ", "Kalori"));

			assertThat(mapping).containsEntry("fatG", 2);
			assertThat(mapping).containsEntry("calories", 3);
		}

		@Test
		void shouldNeverMapTwoFieldsToTheSameColumn() {
			var mapping = CsvColumnMatcher.guessMeasurementColumns(
					List.of("Tarih", "Kilo", "Bel", "Bel Çevresi"));

			assertThat(mapping.values()).doesNotHaveDuplicates();
		}
	}

	@Nested
	class Intake {

		@Test
		void shouldMatchTurkishHeaders() {
			var mapping = CsvColumnMatcher.guessIntakeColumns(
					List.of("Tarih", "Kalori", "Protein", "Karbonhidrat", "Yağ", "Sodyum"));

			assertThat(mapping)
					.containsEntry("consumedOn", 0)
					.containsEntry("calories", 1)
					.containsEntry("proteinG", 2)
					.containsEntry("carbsG", 3)
					.containsEntry("fatG", 4)
					.containsEntry("sodiumMg", 5);
		}

		@Test
		void shouldMatchTheHeadersTrackingAppsExport() {
			var mapping = CsvColumnMatcher.guessIntakeColumns(
					List.of("Date", "Energy (kcal)", "Protein (g)", "Carbohydrates (g)", "Fat (g)", "Sodium (mg)"));

			assertThat(mapping)
					.containsEntry("consumedOn", 0)
					.containsEntry("calories", 1)
					.containsEntry("proteinG", 2)
					.containsEntry("carbsG", 3)
					.containsEntry("fatG", 4)
					.containsEntry("sodiumMg", 5);
		}

		@Test
		void shouldMatchTheShortFormsPeopleType() {
			var mapping = CsvColumnMatcher.guessIntakeColumns(List.of("Gün", "kcal", "KH", "Na"));

			assertThat(mapping)
					.containsEntry("consumedOn", 0)
					.containsEntry("calories", 1)
					.containsEntry("carbsG", 2)
					.containsEntry("sodiumMg", 3);
		}
	}
}
