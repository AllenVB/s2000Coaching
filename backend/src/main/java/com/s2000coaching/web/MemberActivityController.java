package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberActivityService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.ActivityResponse;
import com.s2000coaching.web.dto.UpsertActivityRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Steps and cardio: the member's own log, and the coach's read of any member's. */
@RestController
public class MemberActivityController {

	private final MemberActivityService activityService;
	private final MemberAuthService memberAuthService;

	public MemberActivityController(MemberActivityService activityService, MemberAuthService memberAuthService) {
		this.activityService = activityService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/activity")
	public List<ActivityResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return activityService.listForMember(memberAuthService.requireMember(authorization).getId());
	}

	@PostMapping("/api/member/activity")
	public ActivityResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpsertActivityRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return activityService.save(member, request);
	}

	@DeleteMapping("/api/member/activity/{activityId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID activityId) {
		activityService.deleteOwn(memberAuthService.requireMember(authorization), activityId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/admin/members/{memberId}/activity")
	public List<ActivityResponse> listAsCoach(@PathVariable UUID memberId) {
		return activityService.listForMemberAsCoach(memberId);
	}
}
