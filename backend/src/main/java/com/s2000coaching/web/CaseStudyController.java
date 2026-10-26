package com.s2000coaching.web;

import com.s2000coaching.service.CaseStudyPhotoService;
import com.s2000coaching.service.CaseStudyService;
import com.s2000coaching.service.ProgressPhotoService;
import com.s2000coaching.web.dto.PublicCaseStudyResponse;
import com.s2000coaching.web.dto.PublicCaseStudySummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public read of a case study, for the Tecrübelerim section.
 *
 * <p>Both routes go through the published-only queries. An unpublished draft is not reachable
 * here even with its exact slug, which is the only reason it is safe for the coach to import
 * a messy file and look at the result before deciding to show it.
 */
@RestController
public class CaseStudyController {

	private final CaseStudyService caseStudyService;
	private final CaseStudyPhotoService photoService;

	public CaseStudyController(CaseStudyService caseStudyService, CaseStudyPhotoService photoService) {
		this.caseStudyService = caseStudyService;
		this.photoService = photoService;
	}

	@GetMapping("/api/case-studies")
	public List<PublicCaseStudySummaryResponse> list() {
		return caseStudyService.listPublished();
	}

	@GetMapping("/api/case-studies/{slug}")
	public PublicCaseStudyResponse read(@PathVariable String slug) {
		return caseStudyService.readPublished(slug);
	}

	/**
	 * A published photo. 404 unless the study is published AND its photos are switched on.
	 *
	 * <p>Cache decision: {@code private, no-cache}. Not {@code public}: a shared cache or CDN
	 * would keep serving a body photo after the coach switched photos off, and nothing the
	 * coach does afterwards could pull it back. Not {@code max-age}: even a browser's own copy
	 * would outlive the switch for that long. {@code no-cache} makes the browser ask every time,
	 * so unpublishing takes effect on the next view. The cost is a refetch per image per view,
	 * a handful of files on a page few people open — a fair price for a switch that works.
	 * {@code nosniff} keeps a browser from second-guessing the declared type.
	 */
	@GetMapping("/api/case-studies/{slug}/photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> photo(@PathVariable String slug, @PathVariable UUID photoId) {
		return imageResponse(photoService.readPublic(slug, photoId));
	}

	static ResponseEntity<InputStreamResource> imageResponse(ProgressPhotoService.StoredPhoto photo) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(photo.contentType()))
				.contentLength(photo.sizeBytes())
				.header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
				.header("X-Content-Type-Options", "nosniff")
				.body(new InputStreamResource(photo.content()));
	}
}
