package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.TrainingStatsCalculator;
import com.s2000coaching.service.TrainingStatsService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Records, volume and suggestions: the member's own, and the coach's read of any member's. */
@RestController
public class TrainingStatsController {

	private final TrainingStatsService statsService;
	private final MemberAuthService memberAuthService;

	public TrainingStatsController(TrainingStatsService statsService, MemberAuthService memberAuthService) {
		this.statsService = statsService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/training-stats")
	public TrainingStatsCalculator.Result mine(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return statsService.forMember(member.getId());
	}

	@GetMapping("/api/admin/members/{memberId}/training-stats")
	public TrainingStatsCalculator.Result ofMember(@PathVariable UUID memberId) {
		return statsService.forMemberAsCoach(memberId);
	}
}
