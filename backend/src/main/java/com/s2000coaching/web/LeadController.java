package com.s2000coaching.web;

import com.s2000coaching.domain.LeadStatus;
import com.s2000coaching.service.LeadService;
import com.s2000coaching.web.dto.CreateLeadRequest;
import com.s2000coaching.web.dto.LeadResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pre-assessment requests: public to submit, coach-only to read (behind the session gate in {@code WebConfig}). */
@RestController
public class LeadController {

	private final LeadService leadService;

	public LeadController(LeadService leadService) {
		this.leadService = leadService;
	}

	@PostMapping("/api/leads")
	public ResponseEntity<Void> submit(@Valid @RequestBody CreateLeadRequest request, HttpServletRequest http) {
		leadService.submit(request, clientAddress(http));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/admin/leads")
	public List<LeadResponse> list() {
		return leadService.list();
	}

	@PutMapping("/api/admin/leads/{id}/status")
	public LeadResponse setStatus(@PathVariable UUID id, @RequestParam LeadStatus status) {
		return leadService.setStatus(id, status);
	}

	@DeleteMapping("/api/admin/leads/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		leadService.delete(id);
		return ResponseEntity.noContent().build();
	}

	private static String clientAddress(HttpServletRequest http) {
		String forwarded = http.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return http.getRemoteAddr();
	}
}
