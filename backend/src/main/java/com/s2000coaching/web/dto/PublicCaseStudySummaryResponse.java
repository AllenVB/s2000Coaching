package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CaseStudy;
import java.time.LocalDate;

/** A published study in a list, with just enough to decide whether to open it. */
public record PublicCaseStudySummaryResponse(
		String slug,
		String title,
		String summary,
		LocalDate firstDate,
		LocalDate lastDate,
		int measurementDays,
		int intakeDays) {

	public static PublicCaseStudySummaryResponse from(CaseStudy study, LocalDate firstDate,
			LocalDate lastDate, int measurementDays, int intakeDays) {
		return new PublicCaseStudySummaryResponse(study.getSlug(), study.getTitle(), study.getSummary(),
				firstDate, lastDate, measurementDays, intakeDays);
	}
}
