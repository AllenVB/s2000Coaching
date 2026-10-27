package com.s2000coaching.web;

import com.s2000coaching.service.MemberIntakeService;
import com.s2000coaching.web.dto.IntakeResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Coach-side read of a member's food log. Behind the coach session gate in {@code WebConfig}. */
@RestController
public class AdminIntakeController {

	private final MemberIntakeService intakeService;

	public AdminIntakeController(MemberIntakeService intakeService) {
		this.intakeService = intakeService;
	}

	@GetMapping("/api/admin/members/{memberId}/intake")
	public List<IntakeResponse> list(@PathVariable UUID memberId) {
		return intakeService.listForMemberAsCoach(memberId);
	}
}
