package com.s2000coaching.repository;

import com.s2000coaching.domain.ExperienceCategory;
import com.s2000coaching.domain.ExperienceNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceNoteRepository extends JpaRepository<ExperienceNote, UUID> {

	List<ExperienceNote> findAllByOrderByCategoryAsc();

	Optional<ExperienceNote> findByCategory(ExperienceCategory category);
}
