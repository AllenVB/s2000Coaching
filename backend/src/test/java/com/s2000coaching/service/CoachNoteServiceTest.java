package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachNote;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CoachNoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachNoteServiceTest {

	@Mock
	private CoachNoteRepository noteRepository;

	@Mock
	private MemberService memberService;

	private CoachNoteService coachNoteService;
	private Member member;

	@BeforeEach
	void setUp() {
		coachNoteService = new CoachNoteService(noteRepository, memberService);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
	}

	@Test
	void shouldTrimAndStoreANote() {
		when(memberService.requireMember(member.getId())).thenReturn(member);
		when(noteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = coachNoteService.addNote(member.getId(), "  Omuz ağrısı var  ");

		assertThat(response.body()).isEqualTo("Omuz ağrısı var");
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	void shouldListNewestFirstForAnExistingMember() {
		Instant now = Instant.now();
		when(memberService.requireMember(member.getId())).thenReturn(member);
		when(noteRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())).thenReturn(List.of(
				new CoachNote(UUID.randomUUID(), member, "Yeni", now, now),
				new CoachNote(UUID.randomUUID(), member, "Eski", now.minusSeconds(60), now.minusSeconds(60))));

		assertThat(coachNoteService.listNotes(member.getId()))
				.extracting("body")
				.containsExactly("Yeni", "Eski");
	}

	@Test
	void shouldRejectListingForAnUnknownMember() {
		UUID unknown = UUID.randomUUID();
		when(memberService.requireMember(unknown)).thenThrow(new NotFoundException("Üye bulunamadı: " + unknown));

		assertThatThrownBy(() -> coachNoteService.listNotes(unknown)).isInstanceOf(NotFoundException.class);
		verify(noteRepository, never()).findAllByMemberIdOrderByCreatedAtDesc(any());
	}

	@Test
	void shouldUpdateAndDeleteAnExistingNote() {
		Instant now = Instant.now();
		var note = new CoachNote(UUID.randomUUID(), member, "Eski metin", now, now);
		when(noteRepository.findById(note.getId())).thenReturn(Optional.of(note));

		var updated = coachNoteService.updateNote(note.getId(), " Yeni metin ");
		assertThat(updated.body()).isEqualTo("Yeni metin");
		assertThat(note.getUpdatedAt()).isAfterOrEqualTo(now);

		coachNoteService.deleteNote(note.getId());
		verify(noteRepository).delete(note);
	}

	@Test
	void shouldFailForAnUnknownNote() {
		UUID unknown = UUID.randomUUID();
		when(noteRepository.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> coachNoteService.deleteNote(unknown))
				.isInstanceOf(NotFoundException.class)
				.hasMessageContaining("Not bulunamadı");
	}
}
