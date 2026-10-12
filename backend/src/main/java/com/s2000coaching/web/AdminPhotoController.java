package com.s2000coaching.web;

import com.s2000coaching.service.ProgressPhotoService;
import com.s2000coaching.web.dto.ProgressPhotoResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The coach's view of a member's photos: read and delete, never upload. A photo of someone's
 * body is theirs to put there, so only the member can add one.
 *
 * <p>Every route is behind the coach session gate configured in {@code WebConfig} /
 * {@code CoachAuthInterceptor}.
 */
@RestController
public class AdminPhotoController {

	private final ProgressPhotoService photoService;

	public AdminPhotoController(ProgressPhotoService photoService) {
		this.photoService = photoService;
	}

	@GetMapping("/api/admin/members/{memberId}/photos")
	public List<ProgressPhotoResponse> list(@PathVariable UUID memberId) {
		return photoService.listForMemberAsCoach(memberId);
	}

	@GetMapping("/api/admin/photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> content(@PathVariable UUID photoId) {
		var photo = photoService.readAsCoach(photoId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(photo.contentType()))
				.contentLength(photo.sizeBytes())
				.header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
				.body(new InputStreamResource(photo.content()));
	}

	@DeleteMapping("/api/admin/photos/{photoId}")
	public ResponseEntity<Void> delete(@PathVariable UUID photoId) {
		photoService.deleteAsCoach(photoId);
		return ResponseEntity.noContent().build();
	}
}
