package com.s2000coaching.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.domain.ExperienceCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExperienceCategoryRepositoryTest {

	@Autowired
	private ExperienceCategoryRepository categoryRepository;

	@Test
	void shouldSeedBeslenmeWithTopicsAndProteinNotesInOrder() {
		var categories = categoryRepository.findAllByOrderByCreatedAtAsc();

		assertThat(categories).extracting(ExperienceCategory::getName)
				.containsExactly("Antrenman", "Beslenme", "Kardiyo");

		var beslenme = categories.stream().filter(c -> c.getName().equals("Beslenme")).findFirst().orElseThrow();
		assertThat(beslenme.getTopics()).extracting("name")
				.containsExactly("Protein", "Karbonhidrat", "Yağ", "Sebzeler", "Meyveler", "Supplementler", "Diğer");

		var protein = beslenme.getTopics().get(0);
		assertThat(protein.getNotes()).hasSize(2);
		assertThat(protein.getNotes().get(0).getContent()).contains("1,2-2 gram");
	}
}
