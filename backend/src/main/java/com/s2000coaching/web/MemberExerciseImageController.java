package com.s2000coaching.web;

import com.s2000coaching.service.ExerciseService;
import com.s2000coaching.service.MemberAuthService;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The demonstration image of an exercise, for a signed-in member. It is not personal data, but it is not public either. */
@RestController
public class MemberExerciseImageController {

	private final ExerciseService exerciseService;
	private final MemberAuthService memberAuthService;

	public MemberExerciseImageController(ExerciseService exerciseService, MemberAuthService memberAuthService) {
		this.exerciseService = exerciseService;
		this.memberAuthService = memberAuthService;
	}

	@GetMapping("/api/member/exercises/{exerciseId}/image")
	public ResponseEntity<InputStreamResource> image(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable UUID exerciseId) {
		memberAuthService.requireMember(authorization);
		return ExerciseImages.respond(exerciseService.readImage(exerciseId));
	}
}
