package com.s2000coaching.service;

import com.s2000coaching.domain.CaseStudyPhoto;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.service.analysis.BodyAnalysis.Period;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Matches photos to the periods of the long record: for each period, the photo nearest its
 * start and the photo nearest its end, per pose, side by side.
 *
 * <p>Follows the engine's one rule: where it cannot answer it says why instead of showing
 * something that looks like an answer. A photo months away from a period boundary is not a
 * picture of that boundary, so beyond {@link #MAX_GAP_DAYS} there is no match and a note says
 * so. Every match carries its signed distance in days, and the page prints it — nothing here
 * claims a photo shows a date it was not taken on.
 */
public final class PhotoPairing {

	/** Farther than this and the photo no longer stands for the boundary. */
	public static final long MAX_GAP_DAYS = 45;

	private PhotoPairing() {
	}

	/**
	 * @param daysFromTarget photo date minus the boundary date: negative means the photo was
	 *                       taken before the boundary, positive after
	 */
	public record Match(UUID photoId, LocalDate takenOn, long daysFromTarget) {
	}

	/** One pose within one period. Either side may be null, with {@code note} saying why. */
	public record PosePair(PhotoPose pose, Match start, Match end, String note) {
	}

	public record PeriodPhotos(LocalDate start, LocalDate end, String label, List<PosePair> poses,
			String note) {
	}

	public static List<PeriodPhotos> pair(List<Period> periods, List<CaseStudyPhoto> photos) {
		Map<PhotoPose, List<CaseStudyPhoto>> byPose = new EnumMap<>(PhotoPose.class);
		for (CaseStudyPhoto photo : photos) {
			byPose.computeIfAbsent(photo.getPose(), k -> new ArrayList<>()).add(photo);
		}

		List<PeriodPhotos> result = new ArrayList<>();
		for (Period period : periods) {
			List<PosePair> poses = new ArrayList<>();
			for (var entry : byPose.entrySet()) {
				PosePair pair = pairPose(entry.getKey(), entry.getValue(), period);
				if (pair.start() != null || pair.end() != null) {
					poses.add(pair);
				}
			}
			String note = poses.isEmpty()
					? "Bu dönemin başına ya da sonuna %d günden yakın çekilmiş fotoğraf yok."
							.formatted(MAX_GAP_DAYS)
					: null;
			result.add(new PeriodPhotos(period.start(), period.end(), period.label(), poses, note));
		}
		return result;
	}

	private static PosePair pairPose(PhotoPose pose, List<CaseStudyPhoto> photos, Period period) {
		Match start = nearest(photos, period.start());
		Match end = nearest(photos, period.end());

		if (start != null && end != null && start.photoId().equals(end.photoId())) {
			// One photo cannot be a before and an after of itself.
			return new PosePair(pose, start, null,
					"Dönemin başı ve sonu için aynı fotoğraf en yakın; karşılaştırma yapılamıyor.");
		}
		String note = null;
		if (start == null || end == null) {
			note = start == null
					? "Dönemin başına %d günden yakın fotoğraf yok.".formatted(MAX_GAP_DAYS)
					: "Dönemin sonuna %d günden yakın fotoğraf yok.".formatted(MAX_GAP_DAYS);
		}
		return new PosePair(pose, start, end, note);
	}

	/** Nearest by absolute distance; on a tie the earlier photo, so the result is stable. */
	private static Match nearest(List<CaseStudyPhoto> photos, LocalDate target) {
		return photos.stream()
				.min(Comparator
						.comparingLong((CaseStudyPhoto p) -> Math.abs(ChronoUnit.DAYS.between(target, p.getTakenOn())))
						.thenComparing(CaseStudyPhoto::getTakenOn))
				.map(p -> new Match(p.getId(), p.getTakenOn(), ChronoUnit.DAYS.between(target, p.getTakenOn())))
				.filter(m -> Math.abs(m.daysFromTarget()) <= MAX_GAP_DAYS)
				.orElse(null);
	}
}
