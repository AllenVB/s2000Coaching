package com.s2000coaching.repository;

import com.s2000coaching.domain.Lead;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

	List<Lead> findAllByOrderByCreatedAtDesc();

	long countByStatus(com.s2000coaching.domain.LeadStatus status);

	long countByCreatedAtGreaterThanEqual(java.time.Instant from);
}
