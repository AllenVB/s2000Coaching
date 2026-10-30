package com.s2000coaching.web;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.MemberCodeVerificationRequest;
import com.s2000coaching.web.dto.MemberLoginRequest;
import com.s2000coaching.web.dto.RegisterMemberRequest;
import com.s2000coaching.web.dto.RegistrationResponse;
import com.s2000coaching.web.dto.ResetPasswordRequest;
import com.s2000coaching.web.dto.MemberEmailRequest;
import com.s2000coaching.web.dto.MemberSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Public sign-up and sign-in for the member page: password, or an e-mailed code. */
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

	/** Opens an account; see {@link MemberAuthService#register}. */
	@PostMapping("/api/member/auth/register")
	public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterMemberRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(memberAuthService.register(request));
	}

	@PostMapping("/api/member/auth/login")
	public MemberSessionResponse login(@Valid @RequestBody MemberLoginRequest request) {
		return memberAuthService.login(request.email(), request.password());
	}

	/** Chooses a new password with the e-mailed code, and signs the member in. */
	@PostMapping("/api/member/auth/reset-password")
	public MemberSessionResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return memberAuthService.resetPassword(request.email(), request.code(), request.newPassword());
	}

	@PostMapping("/api/member/auth/logout")
	public ResponseEntity<Void> logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		memberAuthService.logout(authorization);
		return ResponseEntity.noContent().build();
	}
}
