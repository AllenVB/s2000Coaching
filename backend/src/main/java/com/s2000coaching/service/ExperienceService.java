package com.s2000coaching.service;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.domain.ExperienceNote;
import com.s2000coaching.exception.ForbiddenException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExperienceNoteRepository;
import com.s2000coaching.web.dto.ExperienceNoteResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {

	private static final Logger log = LoggerFactory.getLogger(ExperienceService.class);
	private static final String INSECURE_DEFAULT_TOKEN = "s2000-dev-edit";

	private final ExperienceNoteRepository experienceNoteRepository;
	private final String editToken;

	public ExperienceService(ExperienceNoteRepository experienceNoteRepository,
			@Value("${app.experience.edit-token}") String editToken) {
		this.experienceNoteRepository = experienceNoteRepository;
		this.editToken = editToken;
		if (INSECURE_DEFAULT_TOKEN.equals(editToken)) {
			log.warn("app.experience.edit-token is using the insecure local-dev default. "
					+ "Set EXPERIENCE_EDIT_TOKEN before deploying anywhere reachable by the public.");
		}
	}

	@Transactional(readOnly = true)
	public List<ExperienceNoteResponse> listAll() {
		return experienceNoteRepository.findAllByOrderByCategoryAsc().stream()
				.map(ExperienceNoteResponse::from)
				.toList();
	}

	@Transactional
	public ExperienceNoteResponse update(ExperienceCategory category, String content, String providedToken) {
		if (!isValidToken(providedToken)) {
			throw new ForbiddenException("Düzenleme parolası geçersiz");
		}
		ExperienceNote note = experienceNoteRepository.findByCategory(category)
				.orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + category));
		note.setContent(content);
		note.setUpdatedAt(Instant.now());
		return ExperienceNoteResponse.from(experienceNoteRepository.save(note));
	}

	private boolean isValidToken(String providedToken) {
		if (providedToken == null) {
			return false;
		}
		byte[] expected = editToken.getBytes(StandardCharsets.UTF_8);
		byte[] actual = providedToken.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}
}
