package com.s2000coaching.repository;

import com.s2000coaching.domain.CaseStudyMeasurement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseStudyMeasurementRepository extends JpaRepository<CaseStudyMeasurement, UUID> {

	List<CaseStudyMeasurement> findAllByCaseStudyIdOrderByMeasuredOnAsc(UUID caseStudyId);

	Optional<CaseStudyMeasurement> findByCaseStudyIdAndMeasuredOn(UUID caseStudyId, LocalDate measuredOn);

	long countByCaseStudyId(UUID caseStudyId);

	void deleteAllByCaseStudyId(UUID caseStudyId);
}
