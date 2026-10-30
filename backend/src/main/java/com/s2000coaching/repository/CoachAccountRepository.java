package com.s2000coaching.repository;

import com.s2000coaching.domain.CoachAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachAccountRepository extends JpaRepository<CoachAccount, UUID> {

	Optional<CoachAccount> findByEmail(String email);

	boolean existsByEmail(String email);
}
