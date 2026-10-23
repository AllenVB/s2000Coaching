package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.MessageTemplate;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MessageTemplateRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageTemplateServiceTest {

	@Mock
	private MessageTemplateRepository templateRepository;

	private MessageTemplateService templateService;

	@BeforeEach
	void setUp() {
		templateService = new MessageTemplateService(templateRepository);
	}

	@Test
	void shouldTrimTitleAndBodyWhenSaving() {
		when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var template = templateService.createTemplate("  Ölçüm hatırlatması  ", "  Ölçümlerini girer misin?  ");

		assertThat(template.title()).isEqualTo("Ölçüm hatırlatması");
		assertThat(template.body()).isEqualTo("Ölçümlerini girer misin?");
	}

	@Test
	void shouldRejectASecondTemplateWithTheSameTitle() {
		when(templateRepository.existsByTitleIgnoreCase("Haftalık kontrol")).thenReturn(true);

		assertThatThrownBy(() -> templateService.createTemplate("Haftalık kontrol", "merhaba"))
				.isInstanceOf(ConflictException.class);
		verify(templateRepository, never()).save(any());
	}

	@Test
	void shouldLetATemplateKeepItsOwnTitleWhenEdited() {
		UUID id = UUID.randomUUID();
		MessageTemplate existing = new MessageTemplate(id, "Tebrikler", "eski metin", Instant.now());
		when(templateRepository.findById(id)).thenReturn(Optional.of(existing));

		var updated = templateService.updateTemplate(id, "tebrikler", "yeni metin");

		assertThat(updated.title()).isEqualTo("tebrikler");
		assertThat(updated.body()).isEqualTo("yeni metin");
	}

	@Test
	void shouldReportAMissingTemplate() {
		UUID id = UUID.randomUUID();
		when(templateRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> templateService.deleteTemplate(id)).isInstanceOf(NotFoundException.class);
	}
}
