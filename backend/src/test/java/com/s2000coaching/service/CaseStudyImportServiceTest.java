package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyIntake;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.CaseStudyIntakeRepository;
import com.s2000coaching.repository.CaseStudyMeasurementRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseStudyImportServiceTest {

	@Mock
	private CaseStudyMeasurementRepository measurementRepository;

	@Mock
	private CaseStudyIntakeRepository intakeRepository;

	private CaseStudyImportService service;
	private CaseStudy study;

	@BeforeEach
	void setUp() {
		service = new CaseStudyImportService(measurementRepository, intakeRepository);
		study = new CaseStudy(UUID.randomUUID(), "kendi-verim", "Kendi Verim", Instant.now());
	}

	private static byte[] csv(String content) {
		return content.getBytes(StandardCharsets.UTF_8);
	}

	private void noExistingMeasurements() {
		when(measurementRepository.findByCaseStudyIdAndMeasuredOn(any(), any()))
				.thenReturn(Optional.empty());
		when(measurementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
	}

	private void noExistingIntake() {
		when(intakeRepository.findByCaseStudyIdAndConsumedOn(any(), any())).thenReturn(Optional.empty());
		when(intakeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
	}

	@Nested
	class Preview {

		@Test
		void shouldGuessColumnsAndShowTheFirstRowsAsParsed() {
			var preview = service.previewMeasurements(csv("""
					Tarih;Kilo;Bel
					01.03.2025;80,4;85,0
					02.03.2025;80,1;85,0
					"""));

			assertThat(preview.headers()).containsExactly("Tarih", "Kilo", "Bel");
			assertThat(preview.guessedMapping())
					.containsEntry("measuredOn", 0)
					.containsEntry("weightKg", 1)
					.containsEntry("waistCm", 2);
			assertThat(preview.totalRows()).isEqualTo(2);
			assertThat(preview.sampleRows().get(0)).containsExactly("01.03.2025", "80,4", "85,0");
			assertThat(preview.availableFields()).contains("neckCm", "hipCm");
		}

		/** Five rows are enough to spot a wrong decimal mark; a whole file is not needed. */
		@Test
		void shouldCapTheSampleAtFiveRows() {
			StringBuilder content = new StringBuilder("Tarih,Kilo\n");
			for (int day = 1; day <= 20; day++) {
				content.append("2025-03-%02d,80.0%n".formatted(day));
			}

			var preview = service.previewMeasurements(csv(content.toString()));

			assertThat(preview.totalRows()).isEqualTo(20);
			assertThat(preview.sampleRows()).hasSize(5);
		}

		@Test
		void shouldRefuseAFileWithTooManyRows() {
			StringBuilder content = new StringBuilder("Tarih,Kilo\n");
			for (int row = 0; row < 5001; row++) {
				content.append("2025-03-01,80.0\n");
			}

			assertThatThrownBy(() -> service.previewMeasurements(csv(content.toString())))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("en fazla 5000");
		}
	}

	@Nested
	class ImportMeasurements {

		@Test
		void shouldStoreEveryMappedColumn() {
			noExistingMeasurements();

			var result = service.importMeasurements(study, csv("""
					Tarih;Kilo;Boyun;Bel
					01.03.2025;80,4;38,5;85,0
					"""), Map.of("measuredOn", 0, "weightKg", 1, "neckCm", 2, "waistCm", 3));

			ArgumentCaptor<CaseStudyMeasurement> saved =
					ArgumentCaptor.forClass(CaseStudyMeasurement.class);
			verify(measurementRepository).save(saved.capture());
			assertThat(saved.getValue().getMeasuredOn()).isEqualTo(LocalDate.of(2025, 3, 1));
			assertThat(saved.getValue().getWeightKg()).isEqualByComparingTo("80.4");
			assertThat(saved.getValue().getNeckCm()).isEqualByComparingTo("38.5");
			assertThat(saved.getValue().getWaistCm()).isEqualByComparingTo("85.0");
			assertThat(result.importedRows()).isEqualTo(1);
			assertThat(result.firstDate()).isEqualTo(LocalDate.of(2025, 3, 1));
			assertThat(result.lastDate()).isEqualTo(LocalDate.of(2025, 3, 1));
		}

		/** Re-uploading a corrected export must update the day, not add a second one. */
		@Test
		void shouldUpdateAnExistingDateInsteadOfDuplicatingIt() {
			CaseStudyMeasurement existing = new CaseStudyMeasurement(UUID.randomUUID(), study,
					LocalDate.of(2025, 3, 1), new BigDecimal("79.00"));
			when(measurementRepository.findByCaseStudyIdAndMeasuredOn(study.getId(),
					LocalDate.of(2025, 3, 1))).thenReturn(Optional.of(existing));

			var result = service.importMeasurements(study, csv("""
					Tarih,Kilo
					2025-03-01,80.4
					"""), Map.of("measuredOn", 0, "weightKg", 1));

			assertThat(existing.getWeightKg()).isEqualByComparingTo("80.4");
			assertThat(result.updatedRows()).isEqualTo(1);
			assertThat(result.importedRows()).isZero();
			verify(measurementRepository, never()).save(any());
		}

		/**
		 * Row numbers in a warning are the coach's spreadsheet row numbers, header included.
		 * A warning pointing at "row 3" that means the fourth line is worse than no warning.
		 */
		@Test
		void shouldNumberSkippedRowsAsTheSpreadsheetDoes() {
			noExistingMeasurements();

			var result = service.importMeasurements(study, csv("""
					Tarih,Kilo
					2025-03-01,80.4
					,80.1
					2025-03-03,
					"""), Map.of("measuredOn", 0, "weightKg", 1));

			assertThat(result.importedRows()).isEqualTo(1);
			assertThat(result.skippedRows()).isEqualTo(2);
			assertThat(result.warnings()).anyMatch(w -> w.startsWith("3. satır"))
					.anyMatch(w -> w.startsWith("4. satır"));
		}

		@Test
		void shouldRefuseWhenARequiredColumnIsNotMapped() {
			assertThatThrownBy(() -> service.importMeasurements(study, csv("""
					Tarih,Kilo
					2025-03-01,80.4
					"""), Map.of("measuredOn", 0)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("weightKg");
		}

		/** A mapping pointing past the last column would silently read nothing at all. */
		@Test
		void shouldRefuseAColumnIndexBeyondTheFile() {
			assertThatThrownBy(() -> service.importMeasurements(study, csv("""
					Tarih,Kilo
					2025-03-01,80.4
					"""), Map.of("measuredOn", 0, "weightKg", 1, "waistCm", 9)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("waistCm");
		}

		@Test
		void shouldRefuseAFileWhereNothingCouldBeRead() {
			assertThatThrownBy(() -> service.importMeasurements(study, csv("""
					Tarih,Kilo
					,
					,
					"""), Map.of("measuredOn", 0, "weightKg", 1)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Sütun eşlemesini kontrol edin");
		}
	}

	@Nested
	class ImportIntake {

		@Test
		void shouldStoreMacrosAndSodiumWhenMapped() {
			noExistingIntake();

			service.importIntake(study, csv("""
					Tarih,Kalori,Protein,Karbonhidrat,Yağ,Sodyum
					2025-03-01,2450,180,240,70,3200
					"""), Map.of("consumedOn", 0, "calories", 1, "proteinG", 2, "carbsG", 3, "fatG", 4,
					"sodiumMg", 5));

			ArgumentCaptor<CaseStudyIntake> saved = ArgumentCaptor.forClass(CaseStudyIntake.class);
			verify(intakeRepository).save(saved.capture());
			assertThat(saved.getValue().getCalories()).isEqualTo(2450);
			assertThat(saved.getValue().getProteinG()).isEqualTo(180);
			assertThat(saved.getValue().getSodiumMg()).isEqualTo(3200);
		}

		/**
		 * A zero in a food log is an unfilled day, not a day of fasting. Kept out because it
		 * would drag every average that touches it towards a deficit that never happened.
		 */
		@Test
		void shouldSkipZeroCalorieDaysAndSaySo() {
			noExistingIntake();

			var result = service.importIntake(study, csv("""
					Tarih,Kalori
					2025-03-01,2450
					2025-03-02,0
					2025-03-03,2500
					"""), Map.of("consumedOn", 0, "calories", 1));

			assertThat(result.importedRows()).isEqualTo(2);
			assertThat(result.skippedRows()).isEqualTo(1);
			assertThat(result.warnings()).anyMatch(w -> w.startsWith("3. satır") && w.contains("sıfır"));
		}

		@Test
		void shouldReportTheRangeItCovered() {
			noExistingIntake();

			var result = service.importIntake(study, csv("""
					Tarih,Kalori
					2025-03-05,2450
					2025-03-01,2500
					2025-03-09,2400
					"""), Map.of("consumedOn", 0, "calories", 1));

			assertThat(result.firstDate()).isEqualTo(LocalDate.of(2025, 3, 1));
			assertThat(result.lastDate()).isEqualTo(LocalDate.of(2025, 3, 9));
		}

		@Test
		void shouldRefuseWhenCaloriesAreNotMapped() {
			assertThatThrownBy(() -> service.importIntake(study, csv("""
					Tarih,Kalori
					2025-03-01,2450
					"""), Map.of("consumedOn", 0)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("calories");
		}
	}
}
