package com.s2000coaching.service;

import com.s2000coaching.repository.ExperienceCategoryRepository;
import com.s2000coaching.web.dto.ExperienceCategoryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only view of the "Tecrübelerim" content. The category → topic → note tree
 * is curated through Liquibase migrations, not runtime writes, so there are no
 * mutation methods and no public write API.
 */
@Service
public class ExperienceService {

	private final ExperienceCategoryRepository categoryRepository;

	public ExperienceService(ExperienceCategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<ExperienceCategoryResponse> listCategories() {
		return categoryRepository.findAllByOrderByCreatedAtAsc().stream()
				.map(ExperienceCategoryResponse::from)
				.toList();
	}
}
