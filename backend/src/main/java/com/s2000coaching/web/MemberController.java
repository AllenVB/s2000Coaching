package com.s2000coaching.web;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberService;
import com.s2000coaching.web.dto.ChangePasswordRequest;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.UpdateOwnProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	/** The member editing their own details; the coach's form is a separate, wider one. */
	@PatchMapping("/api/member/me")
	public MemberResponse updateMe(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpdateOwnProfileRequest request) {
		return memberService.updateOwnProfile(
				memberAuthService.requireMember(authorization).getId(), request);
	}

	/** Sets the member's password, or changes it (the current one is then required). */
	@PutMapping("/api/member/password")
	public ResponseEntity<Void> changePassword(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody ChangePasswordRequest request) {
		memberAuthService.changePassword(memberAuthService.requireMember(authorization).getId(), authorization,
				request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/api/member/reminders")
	public MemberResponse setReminders(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody RemindersRequest request) {
		return memberService.setRemindersEnabled(memberAuthService.requireMember(authorization).getId(),
				request.enabled());
	}

	public record RemindersRequest(boolean enabled) {
	}

	@PutMapping("/api/member/goals")
	public MemberResponse setGoals(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@jakarta.validation.Valid @RequestBody GoalsRequest request) {
		return memberService.setGoals(memberAuthService.requireMember(authorization).getId(), request.goalWeightKg(),
				request.goalBodyFatPercent());
	}

	public record GoalsRequest(
			@jakarta.validation.constraints.DecimalMin("30.0") @jakarta.validation.constraints.DecimalMax("300.0")
			java.math.BigDecimal goalWeightKg,
			@jakarta.validation.constraints.DecimalMin("3.0") @jakarta.validation.constraints.DecimalMax("60.0")
			java.math.BigDecimal goalBodyFatPercent) {
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
