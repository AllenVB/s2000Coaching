package com.s2000coaching.web;

import com.s2000coaching.service.MeasurementService;
import com.s2000coaching.web.dto.MeasurementResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Coach-side read of a member's check-ins. */
@RestController
public class AdminMeasurementController {

	private final MeasurementService measurementService;

	public AdminMeasurementController(MeasurementService measurementService) {
		this.measurementService = measurementService;
	}

	@GetMapping("/api/admin/members/{memberId}/measurements")
	public List<MeasurementResponse> list(@PathVariable UUID memberId) {
		return measurementService.listForMemberAsCoach(memberId);
	}

	@DeleteMapping("/api/admin/measurements/{measurementId}")
	public ResponseEntity<Void> delete(@PathVariable UUID measurementId) {
		measurementService.deleteAsCoach(measurementId);
		return ResponseEntity.noContent().build();
	}
}
