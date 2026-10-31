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

	/** (member id, day, steps) since a date, for every member in one pass. */
	@org.springframework.data.jpa.repository.Query("select a.member.id, a.performedOn, a.steps from MemberActivity a where a.performedOn >= :from and a.steps is not null")
	List<Object[]> recentSteps(@org.springframework.data.repository.query.Param("from") LocalDate from);

	/** Members who have ever written down their steps: nobody is chased for a habit they never started. */
	@org.springframework.data.jpa.repository.Query("select distinct a.member.id from MemberActivity a where a.steps is not null")
	List<UUID> findMemberIdsWithSteps();
}
