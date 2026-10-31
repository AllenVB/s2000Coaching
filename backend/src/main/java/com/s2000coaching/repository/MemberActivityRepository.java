package com.s2000coaching.repository;

import com.s2000coaching.domain.MemberActivity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberActivityRepository extends JpaRepository<MemberActivity, UUID> {

	List<MemberActivity> findAllByMemberIdOrderByPerformedOnDesc(UUID memberId);

	Optional<MemberActivity> findByMemberIdAndPerformedOn(UUID memberId, LocalDate performedOn);

	void deleteAllByMemberId(UUID memberId);
}
