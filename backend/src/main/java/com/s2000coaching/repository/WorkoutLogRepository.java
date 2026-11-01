package com.s2000coaching.repository;

import com.s2000coaching.domain.WorkoutLog;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {

	Optional<WorkoutLog> findByMemberIdAndDayIdAndPerformedOn(UUID memberId, UUID dayId, LocalDate performedOn);

	List<WorkoutLog> findAllByMemberIdOrderByPerformedOnDesc(UUID memberId);

	/**
	 * The member's most recent session of each of the given plan days, with its sets, in one
	 * query. At most one per day: a session is unique per (member, day, date).
	 */
	@Query("""
			select distinct w from WorkoutLog w left join fetch w.sets
			where w.member.id = :memberId and w.day.id in :dayIds
			  and w.performedOn = (select max(x.performedOn) from WorkoutLog x
			                       where x.member.id = w.member.id and x.day.id = w.day.id)
			""")
	List<WorkoutLog> findLatestPerDay(@Param("memberId") UUID memberId, @Param("dayIds") Collection<UUID> dayIds);

	void deleteAllByMemberId(UUID memberId);

	/** Logs point at plan days, so they must go before the days that own them. */
	void deleteAllByDayIdIn(List<UUID> dayIds);

	/** Newest session per member, in one pass: the attention panel must not query per member. */
	@Query("select w.member.id, max(w.performedOn) from WorkoutLog w group by w.member.id")
	List<Object[]> latestPerformedOnPerMember();

	long countByPerformedOnGreaterThanEqual(java.time.LocalDate from);
}
