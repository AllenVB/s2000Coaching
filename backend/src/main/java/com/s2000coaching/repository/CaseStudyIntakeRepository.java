package com.s2000coaching.repository;

import com.s2000coaching.domain.CaseStudyIntake;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseStudyIntakeRepository extends JpaRepository<CaseStudyIntake, UUID> {

	List<CaseStudyIntake> findAllByCaseStudyIdOrderByConsumedOnAsc(UUID caseStudyId);

	Optional<CaseStudyIntake> findByCaseStudyIdAndConsumedOn(UUID caseStudyId, LocalDate consumedOn);

	long countByCaseStudyId(UUID caseStudyId);

	void deleteAllByCaseStudyId(UUID caseStudyId);
}
