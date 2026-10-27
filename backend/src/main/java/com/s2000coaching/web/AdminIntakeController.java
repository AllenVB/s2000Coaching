package com.s2000coaching.web;

import com.s2000coaching.service.MemberAnalysisService;
import com.s2000coaching.service.MemberIntakeService;
import com.s2000coaching.service.analysis.BodyAnalysis;
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
	private final MemberAnalysisService analysisService;

	public AdminIntakeController(MemberIntakeService intakeService, MemberAnalysisService analysisService) {
		this.intakeService = intakeService;
		this.analysisService = analysisService;
	}

	@GetMapping("/api/admin/members/{memberId}/intake")
	public List<IntakeResponse> list(@PathVariable UUID memberId) {
		return intakeService.listForMemberAsCoach(memberId);
	}

	/**
	 * The same analysis the case studies use, run on this member's check-ins and food log. Coach
	 * only: it comes with caveats a member reading it alone would not see as caveats.
	 */
	@GetMapping("/api/admin/members/{memberId}/analysis")
	public BodyAnalysis analysis(@PathVariable UUID memberId) {
		return analysisService.analyzeAsCoach(memberId);
	}
}
