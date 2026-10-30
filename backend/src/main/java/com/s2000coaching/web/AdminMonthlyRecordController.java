package com.s2000coaching.web;

import com.s2000coaching.service.MonthlyRecordService;
import com.s2000coaching.web.dto.MonthlyRecordResponse;
import com.s2000coaching.web.dto.UpsertMonthlyRecordRequest;
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

/** The coach reading and correcting a member's monthly tables. */
@RestController
public class AdminMonthlyRecordController {

	private final MonthlyRecordService recordService;

	public AdminMonthlyRecordController(MonthlyRecordService recordService) {
		this.recordService = recordService;
	}

	@GetMapping("/api/admin/members/{memberId}/monthly-records")
	public List<MonthlyRecordResponse> list(@PathVariable UUID memberId) {
		return recordService.listAsCoach(memberId);
	}

	@PostMapping("/api/admin/members/{memberId}/monthly-records")
	public ResponseEntity<MonthlyRecordResponse> create(@PathVariable UUID memberId,
			@Valid @RequestBody UpsertMonthlyRecordRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(recordService.create(memberId, request));
	}

	@PutMapping("/api/admin/monthly-records/{recordId}")
	public MonthlyRecordResponse update(@PathVariable UUID recordId,
			@Valid @RequestBody UpsertMonthlyRecordRequest request) {
		return recordService.updateAsCoach(recordId, request);
	}

	@DeleteMapping("/api/admin/monthly-records/{recordId}")
	public ResponseEntity<Void> delete(@PathVariable UUID recordId) {
		recordService.deleteAsCoach(recordId);
		return ResponseEntity.noContent().build();
	}
}
