package com.s2000coaching.web;

import com.s2000coaching.service.MembershipService;
import com.s2000coaching.web.dto.GrantMembershipRequest;
import com.s2000coaching.web.dto.MembershipResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Coach-side membership periods. Behind the coach session gate in {@code WebConfig}. */
@RestController
public class AdminMembershipController {

	private final MembershipService membershipService;

	public AdminMembershipController(MembershipService membershipService) {
		this.membershipService = membershipService;
	}

	@GetMapping("/api/admin/members/{memberId}/memberships")
	public List<MembershipResponse> list(@PathVariable UUID memberId) {
		return membershipService.listAsCoach(memberId);
	}

	@PostMapping("/api/admin/members/{memberId}/memberships")
	public MembershipResponse grant(@PathVariable UUID memberId, @Valid @RequestBody GrantMembershipRequest request) {
		return membershipService.grantManually(memberId, request.label(), request.months());
	}

	@DeleteMapping("/api/admin/memberships/{membershipId}")
	public ResponseEntity<Void> delete(@PathVariable UUID membershipId) {
		membershipService.delete(membershipId);
		return ResponseEntity.noContent().build();
	}
}
