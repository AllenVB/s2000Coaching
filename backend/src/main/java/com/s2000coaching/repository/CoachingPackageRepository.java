package com.s2000coaching.repository;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.PackageCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachingPackageRepository extends JpaRepository<CoachingPackage, UUID> {

	List<CoachingPackage> findByActiveTrueOrderByDisplayOrderAsc();

	List<CoachingPackage> findByActiveTrueAndCategoryOrderByDisplayOrderAsc(PackageCategory category);

	Optional<CoachingPackage> findByIdAndActiveTrue(UUID id);
}
