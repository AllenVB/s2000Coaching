package com.s2000coaching.service;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.domain.ExperienceItem;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExperienceCategoryRepository;
import com.s2000coaching.repository.ExperienceItemRepository;
import com.s2000coaching.web.dto.ExperienceCategoryResponse;
import com.s2000coaching.web.dto.ExperienceItemResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {

	private final ExperienceCategoryRepository categoryRepository;
	private final ExperienceItemRepository itemRepository;

	public ExperienceService(ExperienceCategoryRepository categoryRepository,
			ExperienceItemRepository itemRepository) {
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
	}

	@Transactional(readOnly = true)
	public List<ExperienceCategoryResponse> listCategories() {
		return categoryRepository.findAllByOrderByCreatedAtAsc().stream()
				.map(ExperienceCategoryResponse::from)
				.toList();
	}

	@Transactional
	public ExperienceCategoryResponse createCategory(String name) {
		ExperienceCategory category = new ExperienceCategory(UUID.randomUUID(), name, Instant.now());
		return ExperienceCategoryResponse.from(categoryRepository.save(category));
	}

	@Transactional
	public ExperienceCategoryResponse renameCategory(UUID categoryId, String name) {
		ExperienceCategory category = getCategoryOrThrow(categoryId);
		category.setName(name);
		return ExperienceCategoryResponse.from(category);
	}

	@Transactional
	public void deleteCategory(UUID categoryId) {
		ExperienceCategory category = getCategoryOrThrow(categoryId);
		categoryRepository.delete(category);
	}

	@Transactional
	public ExperienceItemResponse createItem(UUID categoryId, String title, String description) {
		ExperienceCategory category = getCategoryOrThrow(categoryId);
		Instant now = Instant.now();
		ExperienceItem item = new ExperienceItem(UUID.randomUUID(), category, title, description, now, now);
		return ExperienceItemResponse.from(itemRepository.save(item));
	}

	@Transactional
	public ExperienceItemResponse updateItem(UUID itemId, String title, String description) {
		ExperienceItem item = getItemOrThrow(itemId);
		item.setTitle(title);
		item.setDescription(description);
		item.setUpdatedAt(Instant.now());
		return ExperienceItemResponse.from(item);
	}

	@Transactional
	public void deleteItem(UUID itemId) {
		ExperienceItem item = getItemOrThrow(itemId);
		itemRepository.delete(item);
	}

	private ExperienceCategory getCategoryOrThrow(UUID categoryId) {
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + categoryId));
	}

	private ExperienceItem getItemOrThrow(UUID itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Tecrübe bulunamadı: " + itemId));
	}
}
