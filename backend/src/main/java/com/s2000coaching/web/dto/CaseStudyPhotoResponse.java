package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CaseStudyPhoto;
import com.s2000coaching.domain.PhotoPose;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CaseStudyPhotoResponse(UUID id, LocalDate takenOn, PhotoPose pose, long sizeBytes,
		Instant createdAt) {

	public static CaseStudyPhotoResponse from(CaseStudyPhoto photo) {
		return new CaseStudyPhotoResponse(photo.getId(), photo.getTakenOn(), photo.getPose(),
				photo.getSizeBytes(), photo.getCreatedAt());
	}
}
