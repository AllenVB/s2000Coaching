package com.s2000coaching.web;

import com.s2000coaching.service.ExerciseService.StoredImage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** How an exercise image goes out: cacheable by the signed-in person's own browser, never by a proxy. */
final class ExerciseImages {

	private ExerciseImages() {
	}

	static ResponseEntity<InputStreamResource> respond(StoredImage image) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.contentType()))
				.contentLength(image.sizeBytes())
				.header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
				.body(new InputStreamResource(image.content()));
	}
}
