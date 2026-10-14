package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.Sex;
import com.s2000coaching.service.analysis.BodyAnalysis;
import java.time.Instant;

/**
 * A published case study as the public page receives it.
 *
 * <p>Deliberately narrower than {@link CaseStudyResponse}: no id, and no birth year. Height
 * and sex stay because the page has to be able to say which body-fat formula was used and
 * why, and that sentence makes no sense without them. The birth year would add nothing to
 * the reader and is the one field here that identifies a person rather than describing a
 * measurement.
 */
public record PublicCaseStudyResponse(
		String slug,
		String title,
		String summary,
		Integer heightCm,
		Sex sex,
		Instant updatedAt,
		BodyAnalysis analysis) {

	public static PublicCaseStudyResponse from(CaseStudy study, BodyAnalysis analysis) {
		return new PublicCaseStudyResponse(study.getSlug(), study.getTitle(), study.getSummary(),
				study.getHeightCm(), study.getSex(), study.getUpdatedAt(), analysis);
	}
}
