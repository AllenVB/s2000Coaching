package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.Sex;
import java.time.Instant;
import java.util.UUID;

public record CaseStudyResponse(
		UUID id,
		String slug,
		String title,
		String summary,
		Integer heightCm,
		Sex sex,
		Integer birthYear,
		boolean published,
		long measurementCount,
		long intakeCount,
		Instant createdAt,
		Instant updatedAt) {

	public static CaseStudyResponse from(CaseStudy study, long measurementCount, long intakeCount) {
		return new CaseStudyResponse(study.getId(), study.getSlug(), study.getTitle(), study.getSummary(),
				study.getHeightCm(), study.getSex(), study.getBirthYear(), study.isPublished(),
				measurementCount, intakeCount, study.getCreatedAt(), study.getUpdatedAt());
	}
}
