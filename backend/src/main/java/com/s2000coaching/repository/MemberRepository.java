package com.s2000coaching.repository;

import com.s2000coaching.domain.Member;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

	List<Member> findAllByOrderByCreatedAtDesc();

	Optional<Member> findByEmail(String email);

	boolean existsByEmail(String email);
}
