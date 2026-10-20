package com.s2000coaching.repository;

import com.s2000coaching.domain.CoachSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachSessionRepository extends JpaRepository<CoachSession, UUID> {

	Optional<CoachSession> findByTokenHash(String tokenHash);

	void deleteAllByCoachId(UUID coachId);

	int deleteByExpiresAtBefore(Instant cutoff);
}
