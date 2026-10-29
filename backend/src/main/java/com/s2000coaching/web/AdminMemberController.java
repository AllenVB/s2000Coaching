package com.s2000coaching.web;

import com.s2000coaching.service.MemberService;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.UpdateMemberRequest;
import com.s2000coaching.web.dto.CreateMemberResponse;
import com.s2000coaching.web.dto.ResendCodeResponse;
import com.s2000coaching.web.dto.VerificationCodeRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management page API. Every route is behind the coach session gate configured in
 * {@code WebConfig} / {@code CoachAuthInterceptor}.
 */
@RestController
public class AdminMemberController {

	private final MemberService memberService;

	public AdminMemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping("/api/admin/members")
	public List<MemberResponse> listMembers() {
		return memberService.listMembers();
	}

	@GetMapping("/api/admin/members/{memberId}")
	public MemberResponse getMember(@PathVariable UUID memberId) {
		return memberService.getMember(memberId);
	}

	@PutMapping("/api/admin/members/{memberId}")
	public MemberResponse updateMember(
			@PathVariable UUID memberId,
			@Valid @RequestBody UpdateMemberRequest request) {
		return memberService.updateMember(memberId, request);
	}

	@PostMapping("/api/admin/members")
	public ResponseEntity<CreateMemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(memberService.createMember(request));
	}

	/**
	 * Mails a new registration code to a member who has not confirmed yet, and says whether the
	 * mail went out: a code that never left is the coach's to fix, not the member's to guess.
	 */
	@PostMapping("/api/admin/members/{memberId}/verification-code")
	public ResendCodeResponse resendVerificationCode(@PathVariable UUID memberId) {
		return new ResendCodeResponse(memberService.resendRegistrationCode(memberId));
	}

	/** Completes the registration with the code the member received by e-mail. */
	@PostMapping("/api/admin/members/{memberId}/verification")
	public MemberResponse completeRegistration(
			@PathVariable UUID memberId,
			@Valid @RequestBody VerificationCodeRequest request) {
		return memberService.completeRegistration(memberId, request.code());
	}

	@DeleteMapping("/api/admin/members/{memberId}")
	public ResponseEntity<Void> deleteMember(@PathVariable UUID memberId) {
		memberService.deleteMember(memberId);
		return ResponseEntity.noContent().build();
	}
}
