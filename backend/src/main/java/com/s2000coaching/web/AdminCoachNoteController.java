package com.s2000coaching.web;

import com.s2000coaching.service.CoachNoteService;
import com.s2000coaching.web.dto.CoachNoteResponse;
import com.s2000coaching.web.dto.UpsertCoachNoteRequest;
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
public class AdminCoachNoteController {

	private final CoachNoteService coachNoteService;

	public AdminCoachNoteController(CoachNoteService coachNoteService) {
		this.coachNoteService = coachNoteService;
	}

	@GetMapping("/api/admin/members/{memberId}/notes")
	public List<CoachNoteResponse> listNotes(@PathVariable UUID memberId) {
		return coachNoteService.listNotes(memberId);
	}

	@PostMapping("/api/admin/members/{memberId}/notes")
	public ResponseEntity<CoachNoteResponse> addNote(
			@PathVariable UUID memberId,
			@Valid @RequestBody UpsertCoachNoteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(coachNoteService.addNote(memberId, request.body()));
	}

	@PutMapping("/api/admin/notes/{noteId}")
	public CoachNoteResponse updateNote(
			@PathVariable UUID noteId,
			@Valid @RequestBody UpsertCoachNoteRequest request) {
		return coachNoteService.updateNote(noteId, request.body());
	}

	@DeleteMapping("/api/admin/notes/{noteId}")
	public ResponseEntity<Void> deleteNote(@PathVariable UUID noteId) {
		coachNoteService.deleteNote(noteId);
		return ResponseEntity.noContent().build();
	}
}
