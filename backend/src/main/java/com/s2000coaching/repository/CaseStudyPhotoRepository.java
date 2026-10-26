package com.s2000coaching.repository;

import com.s2000coaching.domain.CaseStudyPhoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseStudyPhotoRepository extends JpaRepository<CaseStudyPhoto, UUID> {

	List<CaseStudyPhoto> findAllByCaseStudyIdOrderByTakenOnAsc(UUID caseStudyId);

	/** Scoped by study so a photo id from another study (or none) can never be served. */
	Optional<CaseStudyPhoto> findByIdAndCaseStudyId(UUID id, UUID caseStudyId);

	long countByCaseStudyId(UUID caseStudyId);

	void deleteAllByCaseStudyId(UUID caseStudyId);
}
