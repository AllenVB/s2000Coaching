package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.domain.Sex;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CaseStudyIntakeRepository;
import com.s2000coaching.repository.CaseStudyMeasurementRepository;
import com.s2000coaching.repository.CaseStudyRepository;
import com.s2000coaching.web.dto.PublicCaseStudyResponse;
import com.s2000coaching.web.dto.UpsertCaseStudyRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseStudyServiceTest {

	@Mock
	private CaseStudyRepository studyRepository;

	@Mock
	private CaseStudyMeasurementRepository measurementRepository;

	@Mock
	private CaseStudyIntakeRepository intakeRepository;

	private CaseStudyService service;
	private CaseStudy study;

	@BeforeEach
	void setUp() {
		service = new CaseStudyService(studyRepository, measurementRepository, intakeRepository);
		study = new CaseStudy(UUID.randomUUID(), "kendi-verim", "Kendi Verim", Instant.now());
	}

	private void studyExists() {
		when(studyRepository.findById(study.getId())).thenReturn(Optional.of(study));
	}

	@Nested
	class Slugs {

		/**
		 * Turkish letters must survive as readable ASCII. Left to a plain accent-stripping
		 * pass, {@code ı} and {@code ş} have no base letter to fall back to and vanish
		 * outright, so "Işık Şubat" would become a URL reading "k-ubat".
		 */
		@ParameterizedTest
		@CsvSource({
				"Kendi Verim,kendi-verim",
				"Işık Şubat,isik-subat",
				"İki Yıllık Değişim,iki-yillik-degisim",
				"Çağrı'nın Günlüğü,cagri-nin-gunlugu",
				"  Boşluklu   Başlık  ,bosluklu-baslik",
				"2024 — 2025 Özet,2024-2025-ozet",
				"???,vaka"
		})
		void shouldBuildReadableAsciiSlugs(String title, String expected) {
			assertThat(CaseStudyService.slugify(title)).isEqualTo(expected);
		}

		@Test
		void shouldAddASuffixWhenTheSlugIsTaken() {
			when(studyRepository.existsBySlug("kendi-verim")).thenReturn(true);
			when(studyRepository.existsBySlug("kendi-verim-2")).thenReturn(false);
			when(studyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

			service.create(new UpsertCaseStudyRequest("Kendi Verim", null, null, null, null));

			ArgumentCaptor<CaseStudy> saved = ArgumentCaptor.forClass(CaseStudy.class);
			verify(studyRepository).save(saved.capture());
			assertThat(saved.getValue().getSlug()).isEqualTo("kendi-verim-2");
		}

		@Test
		void shouldRefuseWhenNoUniqueSlugCanBeFound() {
			when(studyRepository.existsBySlug(anyString())).thenReturn(true);

			assertThatThrownBy(() -> service.create(
					new UpsertCaseStudyRequest("Kendi Verim", null, null, null, null)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("benzersiz bir adres");
		}
	}

	@Nested
	class Publishing {

		/** An empty study would publish a page of headings with nothing underneath. */
		@Test
		void shouldRefuseToPublishWithoutAnyMeasurements() {
			studyExists();
			when(measurementRepository.countByCaseStudyId(study.getId())).thenReturn(0L);

			assertThatThrownBy(() -> service.setPublished(study.getId(), true))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("en az bir ölçüm");
			assertThat(study.isPublished()).isFalse();
		}

		@Test
		void shouldPublishOnceDataExists() {
			studyExists();
			when(measurementRepository.countByCaseStudyId(study.getId())).thenReturn(40L);
			when(intakeRepository.countByCaseStudyId(study.getId())).thenReturn(120L);

			var response = service.setPublished(study.getId(), true);

			assertThat(response.published()).isTrue();
			assertThat(response.measurementCount()).isEqualTo(40);
			assertThat(response.intakeCount()).isEqualTo(120);
		}

		/**
		 * Unpublishing must work whatever the data looks like — it is how a page that turned
		 * out wrong gets taken down in a hurry, and a study with no rows is exactly the case
		 * where someone would want it down.
		 */
		@Test
		void shouldUnpublishEvenWithNoData() {
			study.setPublished(true);
			studyExists();
			when(measurementRepository.countByCaseStudyId(study.getId())).thenReturn(0L);

			var response = service.setPublished(study.getId(), false);

			assertThat(response.published()).isFalse();
			assertThat(study.isPublished()).isFalse();
		}
	}

	@Nested
	class PublicRead {

		/** The published flag is part of the query, not a check someone can forget. */
		@Test
		void shouldNotFindAnUnpublishedStudyBySlug() {
			when(studyRepository.findBySlugAndPublishedIsTrue("kendi-verim")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.readPublished("kendi-verim"))
					.isInstanceOf(NotFoundException.class);
			verify(studyRepository, never()).findBySlug(anyString());
		}

		@Test
		void shouldReturnTheAnalysisWithoutTheBirthYear() {
			study.setPublished(true);
			study.setHeightCm(180);
			study.setSex(Sex.ERKEK);
			study.setBirthYear(1995);
			when(studyRepository.findBySlugAndPublishedIsTrue("kendi-verim")).thenReturn(Optional.of(study));
			when(measurementRepository.findAllByCaseStudyIdOrderByMeasuredOnAsc(study.getId()))
					.thenReturn(List.of(measurement(LocalDate.of(2025, 1, 6), "80.00")));
			when(intakeRepository.findAllByCaseStudyIdOrderByConsumedOnAsc(study.getId()))
					.thenReturn(List.of());

			var response = service.readPublished("kendi-verim");

			assertThat(response.slug()).isEqualTo("kendi-verim");
			assertThat(response.heightCm()).isEqualTo(180);
			assertThat(response.analysis().coverage().measurementDays()).isEqualTo(1);
		}

		/**
		 * Birth year feeds the body-fat formula but is not part of what gets published. The
		 * check is on the record's shape rather than on one response, so the field cannot be
		 * added back later without this failing.
		 */
		@Test
		void shouldNotExposeTheBirthYearPublicly() {
			assertThat(PublicCaseStudyResponse.class.getRecordComponents())
					.extracting(java.lang.reflect.RecordComponent::getName)
					.contains("heightCm", "sex", "analysis")
					.doesNotContain("birthYear", "id");
		}

		@Test
		void shouldSummariseEachPublishedStudyWithItsRange() {
			study.setPublished(true);
			when(studyRepository.findAllByPublishedIsTrueOrderByCreatedAtDesc())
					.thenReturn(List.of(study));
			when(measurementRepository.findAllByCaseStudyIdOrderByMeasuredOnAsc(study.getId()))
					.thenReturn(List.of(measurement(LocalDate.of(2024, 1, 1), "78.00"),
							measurement(LocalDate.of(2025, 12, 29), "81.00")));
			when(intakeRepository.countByCaseStudyId(study.getId())).thenReturn(120L);

			var summaries = service.listPublished();

			assertThat(summaries).hasSize(1);
			assertThat(summaries.get(0).firstDate()).isEqualTo(LocalDate.of(2024, 1, 1));
			assertThat(summaries.get(0).lastDate()).isEqualTo(LocalDate.of(2025, 12, 29));
			assertThat(summaries.get(0).measurementDays()).isEqualTo(2);
			assertThat(summaries.get(0).intakeDays()).isEqualTo(120);
		}
	}

	@Nested
	class Deletion {

		/** The rows reference the study, so deleting it first would fail on the constraint. */
		@Test
		void shouldRemoveTheRowsBeforeTheStudy() {
			studyExists();

			service.delete(study.getId());

			var order = org.mockito.Mockito.inOrder(intakeRepository, measurementRepository,
					studyRepository);
			order.verify(intakeRepository).deleteAllByCaseStudyId(study.getId());
			order.verify(measurementRepository).deleteAllByCaseStudyId(study.getId());
			order.verify(studyRepository).delete(study);
		}

		@Test
		void shouldClearOnlyTheRequestedKind() {
			studyExists();

			service.clearData(study.getId(), true);

			verify(measurementRepository).deleteAllByCaseStudyId(study.getId());
			verify(intakeRepository, never()).deleteAllByCaseStudyId(any());
		}

		@Test
		void shouldRejectAnUnknownStudy() {
			when(studyRepository.findById(study.getId())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.delete(study.getId()))
					.isInstanceOf(NotFoundException.class);
		}
	}

	private CaseStudyMeasurement measurement(LocalDate date, String kg) {
		return new CaseStudyMeasurement(UUID.randomUUID(), study, date, new BigDecimal(kg));
	}
}
