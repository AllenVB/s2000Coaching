package com.s2000coaching.service;

import com.s2000coaching.domain.MessageTemplate;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MessageTemplateRepository;
import com.s2000coaching.web.dto.MessageTemplateResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The coach's reusable replies, offered as chips above the composer. */
@Service
public class MessageTemplateService {

	private final MessageTemplateRepository templateRepository;

	public MessageTemplateService(MessageTemplateRepository templateRepository) {
		this.templateRepository = templateRepository;
	}

	@Transactional(readOnly = true)
	public List<MessageTemplateResponse> listTemplates() {
		return templateRepository.findAllByOrderByTitleAsc().stream()
				.map(MessageTemplateResponse::from)
				.toList();
	}

	@Transactional
	public MessageTemplateResponse createTemplate(String title, String body) {
		String trimmedTitle = title.trim();
		// The chips are picked by title, so two templates sharing one are indistinguishable.
		if (templateRepository.existsByTitleIgnoreCase(trimmedTitle)) {
			throw new ConflictException("Bu başlıkta bir hazır yanıt zaten var: " + trimmedTitle);
		}
		MessageTemplate template = new MessageTemplate(UUID.randomUUID(), trimmedTitle, body.trim(),
				Instant.now());
		return MessageTemplateResponse.from(templateRepository.save(template));
	}

	@Transactional
	public MessageTemplateResponse updateTemplate(UUID templateId, String title, String body) {
		MessageTemplate template = getOrThrow(templateId);
		String trimmedTitle = title.trim();
		if (!template.getTitle().equalsIgnoreCase(trimmedTitle)
				&& templateRepository.existsByTitleIgnoreCase(trimmedTitle)) {
			throw new ConflictException("Bu başlıkta bir hazır yanıt zaten var: " + trimmedTitle);
		}
		template.setTitle(trimmedTitle);
		template.setBody(body.trim());
		template.setUpdatedAt(Instant.now());
		return MessageTemplateResponse.from(template);
	}

	@Transactional
	public void deleteTemplate(UUID templateId) {
		templateRepository.delete(getOrThrow(templateId));
	}

	private MessageTemplate getOrThrow(UUID templateId) {
		return templateRepository.findById(templateId)
				.orElseThrow(() -> new NotFoundException("Hazır yanıt bulunamadı: " + templateId));
	}
}
