package com.s2000coaching.web;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberService;
import com.s2000coaching.web.dto.MemberResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Member area API; the bearer token comes from {@code /api/member/auth/verify-code}. */
@RestController
public class MemberController {

	private final MemberAuthService memberAuthService;
	private final MemberService memberService;

	public MemberController(MemberAuthService memberAuthService, MemberService memberService) {
		this.memberAuthService = memberAuthService;
		this.memberService = memberService;
	}

	@GetMapping("/api/member/me")
	public MemberResponse me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return MemberResponse.from(memberAuthService.requireMember(authorization));
	}

	/**
	 * Records the member's consent to health-data processing. Required before any measurement
	 * can be saved; re-posting is a no-op that keeps the original timestamp.
	 */
	@PostMapping("/api/member/consent/health-data")
	public MemberResponse grantHealthDataConsent(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return memberService.grantHealthDataConsent(memberAuthService.requireMember(authorization).getId());
	}
}
