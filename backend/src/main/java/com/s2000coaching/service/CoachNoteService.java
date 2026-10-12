package com.s2000coaching.service;

import com.s2000coaching.domain.CoachNote;
import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CoachNoteRepository;
import com.s2000coaching.web.dto.CoachNoteResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coach-only notes attached to a member. Reachable from the management page only. */
@Service
public class CoachNoteService {

	private final CoachNoteRepository noteRepository;
	private final MemberService memberService;

	public CoachNoteService(CoachNoteRepository noteRepository, MemberService memberService) {
		this.noteRepository = noteRepository;
		this.memberService = memberService;
	}

	@Transactional(readOnly = true)
	public List<CoachNoteResponse> listNotes(UUID memberId) {
		memberService.requireMember(memberId);
		return noteRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.map(CoachNoteResponse::from)
				.toList();
	}

	@Transactional
	public CoachNoteResponse addNote(UUID memberId, String body) {
		Member member = memberService.requireMember(memberId);
		Instant now = Instant.now();
		CoachNote note = new CoachNote(UUID.randomUUID(), member, body.trim(), now, now);
		return CoachNoteResponse.from(noteRepository.save(note));
	}

	@Transactional
	public CoachNoteResponse updateNote(UUID noteId, String body) {
		CoachNote note = getOrThrow(noteId);
		note.setBody(body.trim());
		note.setUpdatedAt(Instant.now());
		return CoachNoteResponse.from(note);
	}

	@Transactional
	public void deleteNote(UUID noteId) {
		noteRepository.delete(getOrThrow(noteId));
	}

	private CoachNote getOrThrow(UUID noteId) {
		return noteRepository.findById(noteId)
				.orElseThrow(() -> new NotFoundException("Not bulunamadı: " + noteId));
	}
}
