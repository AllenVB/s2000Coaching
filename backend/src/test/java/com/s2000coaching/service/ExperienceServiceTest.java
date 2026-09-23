package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.domain.ExperienceItem;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExperienceCategoryRepository;
import com.s2000coaching.repository.ExperienceItemRepository;
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

	@Mock
	private ExperienceCategoryRepository categoryRepository;

	@Mock
	private ExperienceItemRepository itemRepository;

	private ExperienceService experienceService;

	@BeforeEach
	void setUp() {
		experienceService = new ExperienceService(categoryRepository, itemRepository);
	}

	@Nested
	class Categories {

		@Test
		void shouldCreateCategory() {
			when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = experienceService.createCategory("Yoga");

			assertThat(response.name()).isEqualTo("Yoga");
		}

		@Test
		void shouldRenameExistingCategory() {
			var category = new ExperienceCategory(UUID.randomUUID(), "Eski Ad", Instant.now());
			when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

			var response = experienceService.renameCategory(category.getId(), "Yeni Ad");

			assertThat(response.name()).isEqualTo("Yeni Ad");
			assertThat(category.getName()).isEqualTo("Yeni Ad");
		}

		@Test
		void shouldThrowNotFoundWhenRenamingMissingCategory() {
			UUID missingId = UUID.randomUUID();
			when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> experienceService.renameCategory(missingId, "x"))
					.isInstanceOf(NotFoundException.class);
		}

		@Test
		void shouldDeleteCategory() {
			var category = new ExperienceCategory(UUID.randomUUID(), "Silinecek", Instant.now());
			when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

			experienceService.deleteCategory(category.getId());

			verify(categoryRepository).delete(category);
		}
	}

	@Nested
	class Items {

		@Test
		void shouldCreateItemUnderExistingCategory() {
			var category = new ExperienceCategory(UUID.randomUUID(), "Antrenman", Instant.now());
			when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
			when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = experienceService.createItem(category.getId(), "İlk Maraton", "5 saatte bitirdim.");

			assertThat(response.title()).isEqualTo("İlk Maraton");
			assertThat(response.description()).isEqualTo("5 saatte bitirdim.");
		}

		@Test
		void shouldThrowNotFoundWhenCategoryMissingForNewItem() {
			UUID missingCategoryId = UUID.randomUUID();
			when(categoryRepository.findById(missingCategoryId)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> experienceService.createItem(missingCategoryId, "t", "d"))
					.isInstanceOf(NotFoundException.class);
		}

		@Test
		void shouldUpdateItemContent() {
			var category = new ExperienceCategory(UUID.randomUUID(), "Antrenman", Instant.now());
			var item = new ExperienceItem(UUID.randomUUID(), category, "Eski Başlık", "Eski açıklama", Instant.now(),
					Instant.now());
			when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

			var response = experienceService.updateItem(item.getId(), "Yeni Başlık", "Yeni açıklama");

			assertThat(response.title()).isEqualTo("Yeni Başlık");
			assertThat(response.description()).isEqualTo("Yeni açıklama");
		}

		@Test
		void shouldDeleteItem() {
			var category = new ExperienceCategory(UUID.randomUUID(), "Antrenman", Instant.now());
			var item = new ExperienceItem(UUID.randomUUID(), category, "t", "d", Instant.now(), Instant.now());
			when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

			experienceService.deleteItem(item.getId());

			verify(itemRepository).delete(item);
		}
	}
}
