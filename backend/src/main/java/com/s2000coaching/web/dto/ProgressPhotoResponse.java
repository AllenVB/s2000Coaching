package com.s2000coaching.web.dto;

import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.domain.ProgressPhoto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Photo metadata. The bytes are fetched separately by id, so a list of fifty photos stays a
 * small JSON response and the browser can cache each image on its own.
 */
public record ProgressPhotoResponse(
		UUID id,
		LocalDate takenOn,
		PhotoPose pose,
		String contentType,
		long sizeBytes,
		String note,
		Instant createdAt) {

	public static ProgressPhotoResponse from(ProgressPhoto photo) {
		return new ProgressPhotoResponse(photo.getId(), photo.getTakenOn(), photo.getPose(),
				photo.getContentType(), photo.getSizeBytes(), photo.getNote(), photo.getCreatedAt());
	}
}
