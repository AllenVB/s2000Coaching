package com.s2000coaching.web;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.MemberResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Member area API; the bearer token comes from {@code /api/member/auth/verify-code}. */
@RestController
public class MemberController {

	private final MemberAuthService memberAuthService;

	public MemberController(MemberAuthService memberAuthService) {
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/me")
	public MemberResponse me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return MemberResponse.from(memberAuthService.requireMember(authorization));
	}
}
