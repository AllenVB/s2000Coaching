package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MeasurementService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.MeasurementResponse;
import com.s2000coaching.web.dto.UpsertMeasurementRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The member's own check-ins; every route resolves the member from the bearer token. */
@RestController
public class MemberMeasurementController {

	private final MeasurementService measurementService;
	private final MemberAuthService memberAuthService;

	public MemberMeasurementController(MeasurementService measurementService,
			MemberAuthService memberAuthService) {
		this.measurementService = measurementService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/measurements")
	public List<MeasurementResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Member member = memberAuthService.requireMember(authorization);
		return measurementService.listForMember(member.getId());
	}

	@PostMapping("/api/member/measurements")
	public MeasurementResponse save(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UpsertMeasurementRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		return measurementService.save(member, request);
	}

	@DeleteMapping("/api/member/measurements/{measurementId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID measurementId) {
		Member member = memberAuthService.requireMember(authorization);
		measurementService.deleteOwn(member, measurementId);
		return ResponseEntity.noContent().build();
	}
}
