package com.s2000coaching.web;

import com.s2000coaching.service.ExerciseService;
import com.s2000coaching.web.dto.ExerciseResponse;
import com.s2000coaching.web.dto.UpsertExerciseRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.core.io.InputStreamResource;
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
public class AdminExerciseController {

	private final ExerciseService exerciseService;

	public AdminExerciseController(ExerciseService exerciseService) {
		this.exerciseService = exerciseService;
	}

	@GetMapping("/api/admin/exercises")
	public List<ExerciseResponse> list() {
		return exerciseService.list();
	}

	@PostMapping("/api/admin/exercises")
	public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody UpsertExerciseRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.create(request));
	}

	@PutMapping("/api/admin/exercises/{exerciseId}")
	public ExerciseResponse update(@PathVariable UUID exerciseId,
			@Valid @RequestBody UpsertExerciseRequest request) {
		return exerciseService.update(exerciseId, request);
	}

	/** Multipart "file": the demonstration image shown to members at the start of the exercise. */
	@PutMapping(value = "/api/admin/exercises/{exerciseId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ExerciseResponse setImage(@PathVariable UUID exerciseId, @RequestParam("file") MultipartFile file)
			throws IOException {
		return exerciseService.setImage(exerciseId, file.getBytes());
	}

	@DeleteMapping("/api/admin/exercises/{exerciseId}/image")
	public ExerciseResponse removeImage(@PathVariable UUID exerciseId) {
		return exerciseService.removeImage(exerciseId);
	}

	@GetMapping("/api/admin/exercises/{exerciseId}/image")
	public ResponseEntity<InputStreamResource> image(@PathVariable UUID exerciseId) {
		return ExerciseImages.respond(exerciseService.readImage(exerciseId));
	}

	@DeleteMapping("/api/admin/exercises/{exerciseId}")
	public ResponseEntity<Void> delete(@PathVariable UUID exerciseId) {
		exerciseService.delete(exerciseId);
		return ResponseEntity.noContent().build();
	}
}
