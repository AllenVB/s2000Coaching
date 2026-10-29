package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.Sex;
import com.s2000coaching.service.analysis.BodyAnalysis;
import com.s2000coaching.service.PhotoPairing.PeriodPhotos;
import java.time.Instant;
import java.util.List;

/**
 * A published case study as the public page receives it.
 *
 * <p>Deliberately narrower than {@link CaseStudyResponse}: no id, and no birth year. Height
 * and sex stay because the page has to be able to say which body-fat formula was used and
 * why, and that sentence makes no sense without them. The birth year would add nothing to
 * the reader and is the one field here that identifies a person rather than describing a
 * measurement.
 *
 * <p>{@code photoPeriods} is null unless the coach switched photos on; null is "not shown",
 * an empty list would mean "shown, and there are none".
 */
public record PublicCaseStudyResponse(
		String slug,
		String title,
		String summary,
		Integer heightCm,
		Sex sex,
		Instant updatedAt,
		BodyAnalysis analysis,
		List<PeriodPhotos> photoPeriods) {

	public static PublicCaseStudyResponse from(CaseStudy study, BodyAnalysis analysis,
			List<PeriodPhotos> photoPeriods) {
		return new PublicCaseStudyResponse(study.getSlug(), study.getTitle(), study.getSummary(),
				study.getHeightCm(), study.getSex(), study.getUpdatedAt(), analysis,
				photoPeriods);
	}
}
