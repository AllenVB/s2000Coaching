package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.SuccessStoryService;
import com.s2000coaching.web.dto.CreateStoryRequest;
import com.s2000coaching.web.dto.PublicStoryResponse;
import com.s2000coaching.web.dto.StoryConsentRequest;
import com.s2000coaching.web.dto.StoryPhotoSelection;
import com.s2000coaching.web.dto.StoryResponse;
import com.s2000coaching.web.dto.UpdateStoryRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Success stories: the coach writes them, the member consents (or not), the public reads the
 * published ones. Coach routes are behind the session gate in {@code WebConfig}; member routes
 * resolve the member from the token; the public routes serve published stories only.
 */
@RestController
public class SuccessStoryController {

	private final SuccessStoryService storyService;
	private final MemberAuthService memberAuthService;

	public SuccessStoryController(SuccessStoryService storyService, MemberAuthService memberAuthService) {
		this.storyService = storyService;
		this.memberAuthService = memberAuthService;
	}

	// --- coach ---

	@GetMapping("/api/admin/success-stories")
	public List<StoryResponse> list() {
		return storyService.listForCoach();
	}

	@PostMapping("/api/admin/success-stories")
	public StoryResponse create(@Valid @RequestBody CreateStoryRequest request) {
		return storyService.create(request);
	}

	@PutMapping("/api/admin/success-stories/{id}")
	public StoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStoryRequest request) {
		return storyService.update(id, request);
	}

	@PostMapping("/api/admin/success-stories/{id}/photos")
	public StoryResponse addPhoto(@PathVariable UUID id, @Valid @RequestBody StoryPhotoSelection selection) {
		return storyService.addPhoto(id, selection);
	}

	@DeleteMapping("/api/admin/success-story-photos/{photoId}")
	public StoryResponse removePhoto(@PathVariable UUID photoId) {
		return storyService.removePhoto(photoId);
	}

	@PostMapping("/api/admin/success-stories/{id}/send-for-consent")
	public StoryResponse sendForConsent(@PathVariable UUID id) {
		return storyService.sendForConsent(id);
	}

	@PostMapping("/api/admin/success-stories/{id}/publish")
	public StoryResponse publish(@PathVariable UUID id) {
		return storyService.publish(id);
	}

	@PostMapping("/api/admin/success-stories/{id}/unpublish")
	public StoryResponse unpublish(@PathVariable UUID id) {
		return storyService.unpublish(id);
	}

	@DeleteMapping("/api/admin/success-stories/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		storyService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/admin/success-story-photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> coachPhoto(@PathVariable UUID photoId) {
		return CaseStudyController.imageResponse(storyService.readPhotoAsCoach(photoId));
	}

	// --- member ---

	@GetMapping("/api/member/success-stories")
	public List<StoryResponse> mine(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return storyService.listOwn(memberAuthService.requireMember(authorization));
	}

	@PostMapping("/api/member/success-stories/{id}/consent")
	public StoryResponse consent(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID id, @Valid @RequestBody StoryConsentRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return storyService.consent(member, id);
	}

	@PostMapping("/api/member/success-stories/{id}/decline")
	public StoryResponse decline(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID id) {
		return storyService.decline(memberAuthService.requireMember(authorization), id);
	}

	@PostMapping("/api/member/success-stories/{id}/revoke")
	public StoryResponse revoke(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID id) {
		return storyService.revoke(memberAuthService.requireMember(authorization), id);
	}

	@GetMapping("/api/member/success-story-photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> memberPhoto(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID photoId) {
		Member member = memberAuthService.requireMember(authorization);
		return CaseStudyController.imageResponse(storyService.readPhotoAsMember(member, photoId));
	}

	// --- public ---

	@GetMapping("/api/success-stories")
	public List<PublicStoryResponse> published() {
		return storyService.listPublished();
	}

	/** 404 unless the story is published. Same caching decision as case-study photos. */
	@GetMapping("/api/success-stories/{storyId}/photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> publicPhoto(@PathVariable UUID storyId, @PathVariable UUID photoId) {
		return CaseStudyController.imageResponse(storyService.readPublicPhoto(storyId, photoId));
	}
}
