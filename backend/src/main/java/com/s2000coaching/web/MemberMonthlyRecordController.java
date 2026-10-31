package com.s2000coaching.web;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MonthlyRecordService;
import com.s2000coaching.web.dto.MonthlyRecordResponse;
import com.s2000coaching.web.dto.UpsertMonthlyRecordRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The member's own monthly tables ("Verilerim"). */
@RestController
public class MemberMonthlyRecordController {

	private final MemberAuthService memberAuthService;
	private final MonthlyRecordService recordService;

	public MemberMonthlyRecordController(MemberAuthService memberAuthService, MonthlyRecordService recordService) {
		this.memberAuthService = memberAuthService;
		this.recordService = recordService;
	}

	@GetMapping("/api/member/monthly-records")
	public List<MonthlyRecordResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return recordService.list(memberAuthService.requireMember(authorization).getId());
	}

	@PostMapping("/api/member/monthly-records")
	public ResponseEntity<MonthlyRecordResponse> create(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpsertMonthlyRecordRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(recordService.create(memberAuthService.requireMember(authorization).getId(), request));
	}
}
