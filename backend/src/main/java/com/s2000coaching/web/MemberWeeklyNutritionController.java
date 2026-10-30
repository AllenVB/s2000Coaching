package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.WeeklyNutritionService;
import com.s2000coaching.service.WeeklyNutritionService.PhotoUpload;
import com.s2000coaching.web.dto.WeeklyNutritionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The member's own weekly food album; every route resolves the member from the bearer token. */
@RestController
public class MemberWeeklyNutritionController {

	private final WeeklyNutritionService service;
	private final MemberAuthService memberAuthService;

	public MemberWeeklyNutritionController(WeeklyNutritionService service, MemberAuthService memberAuthService) {
		this.service = service;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/weekly-nutrition")
	public List<WeeklyNutritionResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return service.list(member.getId());
	}

	/**
	 * Creates or replaces one week. Multipart because the photo may come with it; every figure is
	 * checked by the service so a missing or malformed one answers 400 in the same shape.
	 */
	@PutMapping(value = "/api/member/weekly-nutrition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public WeeklyNutritionResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
			@RequestParam("calories") int calories,
			@RequestParam("carbsPercent") int carbsPercent,
			@RequestParam("fatPercent") int fatPercent,
			@RequestParam("proteinPercent") int proteinPercent,
			@RequestParam(value = "photo", required = false) MultipartFile photo,
			@RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto) throws IOException {
		Member member = memberAuthService.requireMember(authorization);
		PhotoUpload upload = photo == null || photo.isEmpty()
				? null
				: new PhotoUpload(photo.getContentType(), photo.getBytes());
		return service.save(member, weekStart, calories, carbsPercent, fatPercent, proteinPercent, upload,
				removePhoto);
	}

	/** Private, like every health-data image: the member's own browser may keep it, no proxy may. */
	@GetMapping("/api/member/weekly-nutrition/{id}/photo")
	public ResponseEntity<InputStreamResource> photo(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID id) {
		Member member = memberAuthService.requireMember(authorization);
		return image(service.readOwnPhoto(member, id));
	}

	@DeleteMapping("/api/member/weekly-nutrition/{id}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID id) {
		Member member = memberAuthService.requireMember(authorization);
		service.deleteOwn(member, id);
		return ResponseEntity.noContent().build();
	}

	static ResponseEntity<InputStreamResource> image(WeeklyNutritionService.StoredPhoto photo) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(photo.contentType()))
				.contentLength(photo.sizeBytes())
				.header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
				.body(new InputStreamResource(photo.content()));
	}
}
