package com.s2000coaching.repository;

import com.s2000coaching.domain.CaseStudy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseStudyRepository extends JpaRepository<CaseStudy, UUID> {

	Optional<CaseStudy> findBySlug(String slug);

	/** The public read: an unpublished study must not be reachable by guessing its slug. */
	Optional<CaseStudy> findBySlugAndPublishedIsTrue(String slug);

	List<CaseStudy> findAllByPublishedIsTrueOrderByCreatedAtDesc();

	List<CaseStudy> findAllByOrderByCreatedAtDesc();

	boolean existsBySlug(String slug);
}
