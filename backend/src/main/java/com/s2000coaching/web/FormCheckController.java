package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.FormCheckService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.CoachReplyRequest;
import com.s2000coaching.web.dto.CreateFormCheckRequest;
import com.s2000coaching.web.dto.FormCheckResponse;
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

/** Form checks: the member's own, and the coach's read and reply. */
@RestController
public class FormCheckController {

	private final FormCheckService formCheckService;
	private final MemberAuthService memberAuthService;

	public FormCheckController(FormCheckService formCheckService, MemberAuthService memberAuthService) {
		this.formCheckService = formCheckService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/form-checks")
	public List<FormCheckResponse> mine(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return formCheckService.listForMember(member.getId());
	}

	@PostMapping("/api/member/form-checks")
	public FormCheckResponse create(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody CreateFormCheckRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return formCheckService.create(member, request);
	}

	@DeleteMapping("/api/member/form-checks/{id}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID id) {
		Member member = memberAuthService.requireMember(authorization);
		formCheckService.deleteOwn(member, id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/admin/members/{memberId}/form-checks")
	public List<FormCheckResponse> ofMember(@PathVariable UUID memberId) {
		return formCheckService.listForMemberAsCoach(memberId);
	}

	@PostMapping("/api/admin/form-checks/{id}/reply")
	public FormCheckResponse reply(@PathVariable UUID id, @Valid @RequestBody CoachReplyRequest request) {
		return formCheckService.reply(id, request.reply());
	}
}
