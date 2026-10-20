package com.s2000coaching.repository;

import com.s2000coaching.domain.MemberSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSessionRepository extends JpaRepository<MemberSession, UUID> {

	Optional<MemberSession> findByTokenHash(String tokenHash);

	void deleteAllByMemberId(UUID memberId);

	int deleteByExpiresAtBefore(Instant cutoff);
}
