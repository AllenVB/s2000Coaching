package com.s2000coaching.web;

import com.s2000coaching.service.ExerciseService;
import com.s2000coaching.web.dto.ExerciseResponse;
import com.s2000coaching.web.dto.UpsertExerciseRequest;
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

	@DeleteMapping("/api/admin/exercises/{exerciseId}")
	public ResponseEntity<Void> delete(@PathVariable UUID exerciseId) {
		exerciseService.delete(exerciseId);
		return ResponseEntity.noContent().build();
	}
}
