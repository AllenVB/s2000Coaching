package com.s2000coaching.web;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.service.ExperienceService;
import com.s2000coaching.web.dto.ExperienceNoteResponse;
import com.s2000coaching.web.dto.UpdateExperienceRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExperienceController {

	private final ExperienceService experienceService;

	public ExperienceController(ExperienceService experienceService) {
		this.experienceService = experienceService;
	}

	@GetMapping("/api/experiences")
	public List<ExperienceNoteResponse> listExperiences() {
		return experienceService.listAll();
	}

	@PutMapping("/api/experiences/{category}")
	public ExperienceNoteResponse updateExperience(
			@PathVariable ExperienceCategory category,
			@Valid @RequestBody UpdateExperienceRequest request,
			@RequestHeader(value = "X-Edit-Token", required = false) String editToken) {
		return experienceService.update(category, request.content(), editToken);
	}
}
