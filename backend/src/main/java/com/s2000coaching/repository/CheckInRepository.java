package com.s2000coaching.repository;

import com.s2000coaching.domain.CheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

	List<CheckIn> findAllByMemberIdOrderByWeekStartDesc(UUID memberId);

	Optional<CheckIn> findByMemberIdAndWeekStart(UUID memberId, LocalDate weekStart);

	void deleteAllByMemberId(UUID memberId);

	/** Members with a check-in the coach has not answered, for the attention panel. */
	@Query("select distinct c.member.id from CheckIn c where c.coachReply is null")
	List<UUID> findMemberIdsWithUnansweredCheckIn();

	/** The newest week each member reported, in one pass for the reminder job. */
	@Query("select c.member.id, max(c.weekStart) from CheckIn c group by c.member.id")
	List<Object[]> latestWeekPerMember();

	long countByWeekStartGreaterThanEqual(java.time.LocalDate from);
}
