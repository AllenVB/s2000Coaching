package com.s2000coaching.web;

import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.CoachLoginRequest;
import com.s2000coaching.web.dto.CoachResponse;
import com.s2000coaching.web.dto.CoachSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Management sign-in. {@code /login} is the one admin route reachable without a session. */
@RestController
public class CoachAuthController {

	private final CoachAuthService coachAuthService;

	public CoachAuthController(CoachAuthService coachAuthService) {
		this.coachAuthService = coachAuthService;
	}

	@PostMapping("/api/admin/auth/login")
	public CoachSessionResponse login(@Valid @RequestBody CoachLoginRequest request) {
		return coachAuthService.login(request.email(), request.password());
	}

	@GetMapping("/api/admin/auth/me")
	public CoachResponse me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return CoachResponse.from(coachAuthService.requireCoach(authorization));
	}

	@PostMapping("/api/admin/auth/logout")
	public ResponseEntity<Void> logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		coachAuthService.logout(authorization);
		return ResponseEntity.noContent().build();
	}
}
