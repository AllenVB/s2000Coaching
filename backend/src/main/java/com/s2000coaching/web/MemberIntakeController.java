package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberIntakeService;
import com.s2000coaching.web.dto.IntakeResponse;
import com.s2000coaching.web.dto.UpsertIntakeRequest;
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

/** The member's own food log; every route resolves the member from the bearer token. */
@RestController
public class MemberIntakeController {

	private final MemberIntakeService intakeService;
	private final MemberAuthService memberAuthService;

	public MemberIntakeController(MemberIntakeService intakeService, MemberAuthService memberAuthService) {
		this.intakeService = intakeService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/intake")
	public List<IntakeResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return intakeService.listForMember(member.getId());
	}

	@PostMapping("/api/member/intake")
	public IntakeResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpsertIntakeRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return intakeService.save(member, request);
	}

	@DeleteMapping("/api/member/intake/{intakeId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID intakeId) {
		Member member = memberAuthService.requireMember(authorization);
		intakeService.deleteOwn(member, intakeId);
		return ResponseEntity.noContent().build();
	}
}
