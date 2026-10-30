package com.s2000coaching.web;

import com.s2000coaching.service.WeeklyNutritionService;
import com.s2000coaching.web.dto.WeeklyNutritionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** What the coach sees of a member's weekly food album: read it, view a photo, remove an entry. */
@RestController
public class AdminWeeklyNutritionController {

	private final WeeklyNutritionService service;

	public AdminWeeklyNutritionController(WeeklyNutritionService service) {
		this.service = service;
	}

	@GetMapping("/api/admin/members/{memberId}/weekly-nutrition")
	public List<WeeklyNutritionResponse> list(@PathVariable UUID memberId) {
		return service.listAsCoach(memberId);
	}

	@GetMapping("/api/admin/weekly-nutrition/{id}/photo")
	public ResponseEntity<InputStreamResource> photo(@PathVariable UUID id) {
		return MemberWeeklyNutritionController.image(service.readPhotoAsCoach(id));
	}

	@DeleteMapping("/api/admin/weekly-nutrition/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.deleteAsCoach(id);
		return ResponseEntity.noContent().build();
	}
}
