package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MembershipService;
import com.s2000coaching.web.dto.MembershipResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The member's own periods, newest first; the first one is the current period. */
@RestController
public class MemberMembershipController {

	private final MembershipService membershipService;
	private final MemberAuthService memberAuthService;

	public MemberMembershipController(MembershipService membershipService, MemberAuthService memberAuthService) {
		this.membershipService = membershipService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/memberships")
	public List<MembershipResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return membershipService.list(member.getId());
	}
}
