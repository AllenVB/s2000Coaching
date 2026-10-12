package com.s2000coaching.web;

import com.s2000coaching.service.ExperienceService;
import com.s2000coaching.web.dto.ExperienceCategoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExperienceController {

	private final ExperienceService experienceService;

	public ExperienceController(ExperienceService experienceService) {
		this.experienceService = experienceService;
	}

	@GetMapping("/api/experience-categories")
	public List<ExperienceCategoryResponse> listCategories() {
		return experienceService.listCategories();
	}
}
