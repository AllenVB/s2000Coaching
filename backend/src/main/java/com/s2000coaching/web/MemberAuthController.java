package com.s2000coaching.web;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.MemberCodeVerificationRequest;
import com.s2000coaching.web.dto.MemberEmailRequest;
import com.s2000coaching.web.dto.MemberSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Public, passwordless sign-in for the member page. */
@RestController
public class MemberAuthController {

	private final MemberAuthService memberAuthService;

	public MemberAuthController(MemberAuthService memberAuthService) {
		this.memberAuthService = memberAuthService;
	}

	/**
	 * Mails a one-time code. Always answers 204, including for unknown addresses, so the
	 * endpoint cannot be used to find out who is a member.
	 */
	@PostMapping("/api/member/auth/request-code")
	public ResponseEntity<Void> requestCode(@Valid @RequestBody MemberEmailRequest request) {
		memberAuthService.requestCode(request.email());
		return ResponseEntity.noContent().build();
	}

	/** Exchanges a one-time code for a session; activates the account if it was still pending. */
	@PostMapping("/api/member/auth/verify-code")
	public MemberSessionResponse verifyCode(@Valid @RequestBody MemberCodeVerificationRequest request) {
		return memberAuthService.verifyCode(request.email(), request.code());
	}

	@PostMapping("/api/member/auth/logout")
	public ResponseEntity<Void> logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		memberAuthService.logout(authorization);
		return ResponseEntity.noContent().build();
	}
}
