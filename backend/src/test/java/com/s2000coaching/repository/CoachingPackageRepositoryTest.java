package com.s2000coaching.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.domain.PackageCategory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CoachingPackageRepositoryTest {

	@org.springframework.beans.factory.annotation.Autowired
	private CoachingPackageRepository packageRepository;

	@Test
	void shouldReturnOnlyActiveKocluckPackagesOrderedByDisplayOrder() {
		var packages = packageRepository.findByActiveTrueAndCategoryOrderByDisplayOrderAsc(PackageCategory.KOCLUK);

		assertThat(packages).hasSize(3);
		assertThat(packages).isSortedAccordingTo((a, b) -> a.getDisplayOrder() - b.getDisplayOrder());
		assertThat(packages.get(1).getSlug()).isEqualTo("kocluk-6-ay");
		assertThat(packages.get(1).getFeatures()).hasSize(5);
	}

	@Test
	void shouldReturnAllActivePackagesWhenCategoryIsNull() {
		var packages = packageRepository.findByActiveTrueOrderByDisplayOrderAsc();

		assertThat(packages).hasSize(7);
	}
}
