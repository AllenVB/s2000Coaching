package com.s2000coaching.web;

import com.s2000coaching.service.ExperienceService;
import com.s2000coaching.web.dto.ExperienceCategoryResponse;
import com.s2000coaching.web.dto.ExperienceItemResponse;
import com.s2000coaching.web.dto.UpsertCategoryRequest;
import com.s2000coaching.web.dto.UpsertItemRequest;
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

	@PostMapping("/api/experience-categories")
	public ResponseEntity<ExperienceCategoryResponse> createCategory(
			@Valid @RequestBody UpsertCategoryRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(experienceService.createCategory(request.name()));
	}

	@PutMapping("/api/experience-categories/{categoryId}")
	public ExperienceCategoryResponse renameCategory(
			@PathVariable UUID categoryId,
			@Valid @RequestBody UpsertCategoryRequest request) {
		return experienceService.renameCategory(categoryId, request.name());
	}

	@DeleteMapping("/api/experience-categories/{categoryId}")
	public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
		experienceService.deleteCategory(categoryId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/experience-categories/{categoryId}/items")
	public ResponseEntity<ExperienceItemResponse> createItem(
			@PathVariable UUID categoryId,
			@Valid @RequestBody UpsertItemRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(experienceService.createItem(categoryId, request.title(), request.description()));
	}

	@PutMapping("/api/experience-items/{itemId}")
	public ExperienceItemResponse updateItem(
			@PathVariable UUID itemId,
			@Valid @RequestBody UpsertItemRequest request) {
		return experienceService.updateItem(itemId, request.title(), request.description());
	}

	@DeleteMapping("/api/experience-items/{itemId}")
	public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
		experienceService.deleteItem(itemId);
		return ResponseEntity.noContent().build();
	}
}
