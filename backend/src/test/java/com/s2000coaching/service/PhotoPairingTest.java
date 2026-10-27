package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.domain.CaseStudyPhoto;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.service.analysis.BodyAnalysis.Period;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhotoPairingTest {

	private static final UUID STUDY = UUID.randomUUID();

	private static CaseStudyPhoto photo(LocalDate date, PhotoPose pose) {
		return new CaseStudyPhoto(UUID.randomUUID(), STUDY, date, pose, "k" + date + pose, "image/jpeg", 1,
				Instant.now());
	}

	private static Period period(LocalDate start, LocalDate end) {
		return new Period(start, end, "KILO_VERME", -5.0, null, null);
	}

	@Test
	void shouldPickTheNearestPhotoToEachBoundaryWithSignedDistance() {
		var early = photo(LocalDate.of(2025, 1, 1), PhotoPose.ON);
		var nearStart = photo(LocalDate.of(2025, 1, 12), PhotoPose.ON); // 9 days before period start
		var nearEnd = photo(LocalDate.of(2025, 4, 3), PhotoPose.ON); // 2 days after end
		var periods = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 1, 21), LocalDate.of(2025, 4, 1))),
				List.of(early, nearStart, nearEnd));

		var pair = periods.get(0).poses().get(0);

		assertThat(pair.start().photoId()).isEqualTo(nearStart.getId());
		assertThat(pair.start().daysFromTarget()).isEqualTo(-9);
		assertThat(pair.end().photoId()).isEqualTo(nearEnd.getId());
		assertThat(pair.end().daysFromTarget()).isEqualTo(2);
		assertThat(pair.note()).isNull();
	}

	/** A photo months from the boundary is not a picture of the boundary. */
	@Test
	void shouldNotMatchAPhotoBeyondTheMaximumGap() {
		var far = photo(LocalDate.of(2025, 1, 1), PhotoPose.ON);
		var periods = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 1))), List.of(far));

		assertThat(periods.get(0).poses()).isEmpty();
		assertThat(periods.get(0).note()).contains("45");
	}

	@Test
	void shouldMatchAtExactlyTheMaximumGapButNotOneDayBeyond() {
		var edge = photo(LocalDate.of(2025, 1, 1).plusDays(45), PhotoPose.ON);
		var beyond = photo(LocalDate.of(2025, 6, 1).plusDays(46), PhotoPose.ON);

		var atEdge = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))), List.of(edge));
		var pastEdge = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 6, 1), LocalDate.of(2026, 12, 31))), List.of(beyond));

		assertThat(atEdge.get(0).poses().get(0).start()).isNotNull();
		assertThat(pastEdge.get(0).poses()).isEmpty();
	}

	@Test
	void shouldNeverUseOnePhotoAsBothBeforeAndAfter() {
		var only = photo(LocalDate.of(2025, 2, 1), PhotoPose.ON);
		var periods = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 1, 25), LocalDate.of(2025, 2, 10))), List.of(only));

		var pair = periods.get(0).poses().get(0);
		assertThat(pair.start()).isNotNull();
		assertThat(pair.end()).isNull();
		assertThat(pair.note()).contains("aynı fotoğraf");
	}

	@Test
	void shouldNeverPairAcrossPoses() {
		var front = photo(LocalDate.of(2025, 1, 20), PhotoPose.ON);
		var back = photo(LocalDate.of(2025, 4, 1), PhotoPose.ARKA);
		var periods = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 1, 21), LocalDate.of(2025, 4, 1))), List.of(front, back));

		var poses = periods.get(0).poses();
		assertThat(poses).hasSize(2);
		for (var pair : poses) {
			// Each pose found only one side; the missing side is explained, never borrowed.
			assertThat(pair.start() == null ^ pair.end() == null).isTrue();
			assertThat(pair.note()).isNotBlank();
		}
	}

	@Test
	void shouldPreferTheEarlierPhotoOnATie() {
		var before = photo(LocalDate.of(2025, 1, 10), PhotoPose.ON);
		var after = photo(LocalDate.of(2025, 1, 20), PhotoPose.ON);
		var periods = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 3, 15))), List.of(after, before));

		assertThat(periods.get(0).poses().get(0).start().photoId()).isEqualTo(before.getId());
	}

	@Test
	void shouldReportEveryPeriodEvenWhenThereAreNoPhotos() {
		var periods = PhotoPairing.pair(
				List.of(period(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1)),
						period(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 1))),
				List.of());

		assertThat(periods).hasSize(2);
		assertThat(periods).allSatisfy(p -> assertThat(p.note()).isNotBlank());
	}
}
