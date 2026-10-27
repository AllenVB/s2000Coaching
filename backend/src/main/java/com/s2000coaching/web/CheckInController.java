package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.CheckInService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.CheckInResponse;
import com.s2000coaching.web.dto.CoachReplyRequest;
import com.s2000coaching.web.dto.UpsertCheckInRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Weekly check-ins: the member's own, and the coach's read and reply. */
@RestController
public class CheckInController {

	private final CheckInService checkInService;
	private final MemberAuthService memberAuthService;

	public CheckInController(CheckInService checkInService, MemberAuthService memberAuthService) {
		this.checkInService = checkInService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/check-ins")
	public List<CheckInResponse> mine(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return checkInService.listForMember(member.getId());
	}

	@PostMapping("/api/member/check-ins")
	public CheckInResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpsertCheckInRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return checkInService.save(member, request);
	}

	@GetMapping("/api/admin/members/{memberId}/check-ins")
	public List<CheckInResponse> ofMember(@PathVariable UUID memberId) {
		return checkInService.listForMemberAsCoach(memberId);
	}

	@PostMapping("/api/admin/check-ins/{checkInId}/reply")
	public CheckInResponse reply(@PathVariable UUID checkInId, @Valid @RequestBody CoachReplyRequest request) {
		return checkInService.reply(checkInId, request.reply());
	}
}
