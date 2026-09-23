package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.domain.ExperienceNote;
import com.s2000coaching.exception.ForbiddenException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExperienceNoteRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

	private static final String EDIT_TOKEN = "correct-token";

	@Mock
	private ExperienceNoteRepository experienceNoteRepository;

	private ExperienceService experienceService;

	@BeforeEach
	void setUp() {
		experienceService = new ExperienceService(experienceNoteRepository, EDIT_TOKEN);
	}

	@Nested
	class Update {

		@Test
		void shouldUpdateContentWhenTokenMatches() {
			ExperienceNote note = new ExperienceNote(UUID.randomUUID(), ExperienceCategory.ANTRENMAN, "eski içerik",
					Instant.now());
			when(experienceNoteRepository.findByCategory(ExperienceCategory.ANTRENMAN)).thenReturn(Optional.of(note));
			when(experienceNoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = experienceService.update(ExperienceCategory.ANTRENMAN, "yeni içerik", EDIT_TOKEN);

			assertThat(response.content()).isEqualTo("yeni içerik");
			assertThat(note.getContent()).isEqualTo("yeni içerik");
		}

		@Test
		void shouldRejectWhenTokenIsWrong() {
			assertThatThrownBy(() -> experienceService.update(ExperienceCategory.ANTRENMAN, "x", "wrong-token"))
					.isInstanceOf(ForbiddenException.class);
		}

		@Test
		void shouldRejectWhenTokenIsMissing() {
			assertThatThrownBy(() -> experienceService.update(ExperienceCategory.ANTRENMAN, "x", null))
					.isInstanceOf(ForbiddenException.class);
		}

		@Test
		void shouldThrowNotFoundWhenCategoryRowIsMissing() {
			when(experienceNoteRepository.findByCategory(ExperienceCategory.KARDIYO)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> experienceService.update(ExperienceCategory.KARDIYO, "x", EDIT_TOKEN))
					.isInstanceOf(NotFoundException.class);
		}
	}
}
