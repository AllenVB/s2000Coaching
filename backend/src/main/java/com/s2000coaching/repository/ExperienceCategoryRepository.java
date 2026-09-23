package com.s2000coaching.repository;

import com.s2000coaching.domain.ExperienceCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceCategoryRepository extends JpaRepository<ExperienceCategory, UUID> {

	List<ExperienceCategory> findAllByOrderByCreatedAtAsc();
}
