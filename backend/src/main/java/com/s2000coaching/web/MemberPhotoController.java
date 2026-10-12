package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.ProgressPhotoService;
import com.s2000coaching.web.dto.ProgressPhotoResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The member's own progress photos; every route resolves the member from the bearer token. */
@RestController
public class MemberPhotoController {

	private final ProgressPhotoService photoService;
	private final MemberAuthService memberAuthService;

	public MemberPhotoController(ProgressPhotoService photoService, MemberAuthService memberAuthService) {
		this.photoService = photoService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/photos")
	public List<ProgressPhotoResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return photoService.listForMember(member.getId());
	}

	@PostMapping(value = "/api/member/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProgressPhotoResponse upload(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam("file") MultipartFile file,
			@RequestParam("takenOn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenOn,
			@RequestParam("pose") PhotoPose pose,
			@RequestParam(value = "note", required = false) String note) throws IOException {
		Member member = memberAuthService.requireMember(authorization);
		return photoService.upload(member, takenOn, pose, file.getContentType(), file.getBytes(), note);
	}

	/**
	 * The image itself. Marked private so it is never cached by a proxy — these are
	 * special-category personal data — but cacheable by the member's own browser, which is
	 * what keeps a comparison view from refetching the same two photos on every render.
	 */
	@GetMapping("/api/member/photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> content(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID photoId) {
		Member member = memberAuthService.requireMember(authorization);
		var photo = photoService.readOwn(member, photoId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(photo.contentType()))
				.contentLength(photo.sizeBytes())
				.header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
				.body(new InputStreamResource(photo.content()));
	}

	@DeleteMapping("/api/member/photos/{photoId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID photoId) {
		Member member = memberAuthService.requireMember(authorization);
		photoService.deleteOwn(member, photoId);
		return ResponseEntity.noContent().build();
	}
}
