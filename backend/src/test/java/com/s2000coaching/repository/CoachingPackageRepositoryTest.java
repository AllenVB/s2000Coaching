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
		// One, three and six months; the three-month tier is the highlighted one.
		assertThat(packages).extracting(p -> p.getSlug()).containsExactly("kocluk-1-ay", "kocluk-3-ay", "kocluk-6-ay");
		assertThat(packages).extracting(p -> p.getPriceMinor()).containsExactly(299000L, 749000L, 1249000L);
		assertThat(packages).extracting(p -> p.getOriginalPriceMinor()).containsExactly(350000L, 1200000L, 1900000L);
		assertThat(packages).extracting(p -> p.isFeatured()).containsExactly(false, true, false);
		assertThat(packages.get(1).getFeatures()).hasSize(5);
	}

	@Test
	void shouldOfferThreeNutritionTiersWithTheSixMonthOneHighlighted() {
		var packages = packageRepository.findByActiveTrueAndCategoryOrderByDisplayOrderAsc(PackageCategory.BESLENME);

		assertThat(packages).extracting(p -> p.getSlug()).containsExactly("beslenme-1-ay", "beslenme-3-ay", "beslenme-6-ay");
		assertThat(packages).extracting(p -> p.getPriceMinor()).containsExactly(129000L, 269000L, 459000L);
		assertThat(packages).extracting(p -> p.getOriginalPriceMinor()).containsExactly(150000L, 360000L, 750000L);
		assertThat(packages).extracting(p -> p.isFeatured()).containsExactly(false, false, true);
	}

	@Test
	void shouldReturnAllActivePackagesWhenCategoryIsNull() {
		var packages = packageRepository.findByActiveTrueOrderByDisplayOrderAsc();

		assertThat(packages).hasSize(6);
	}
}
