package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.QuestionnaireService;
import com.s2000coaching.web.dto.QuestionnaireResponse;
import com.s2000coaching.web.dto.UpsertQuestionnaireRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The starting questionnaire: the member's own, and the coach's read of any member's. */
@RestController
public class QuestionnaireController {

	private final QuestionnaireService questionnaireService;
	private final MemberAuthService memberAuthService;

	public QuestionnaireController(QuestionnaireService questionnaireService, MemberAuthService memberAuthService) {
		this.questionnaireService = questionnaireService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/questionnaire")
	public QuestionnaireResponse mine(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return questionnaireService.get(member.getId());
	}

	@PostMapping("/api/member/questionnaire")
	public QuestionnaireResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpsertQuestionnaireRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return questionnaireService.save(member, request);
	}

	@GetMapping("/api/admin/members/{memberId}/questionnaire")
	public QuestionnaireResponse ofMember(@PathVariable UUID memberId) {
		return questionnaireService.getAsCoach(memberId);
	}
}
