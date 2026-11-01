package com.s2000coaching.repository;

import com.s2000coaching.domain.MemberIntake;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberIntakeRepository extends JpaRepository<MemberIntake, UUID> {

	List<MemberIntake> findAllByMemberIdOrderByConsumedOnDesc(UUID memberId);

	List<MemberIntake> findAllByMemberIdOrderByConsumedOnAsc(UUID memberId);

	Optional<MemberIntake> findByMemberIdAndConsumedOn(UUID memberId, LocalDate consumedOn);

	void deleteAllByMemberId(UUID memberId);

	/** Newest logged day per member, in one pass — the attention panel must not query per member. */
	@Query("""
			select i.member.id, max(i.consumedOn) from MemberIntake i group by i.member.id
			""")
	List<Object[]> latestConsumedOnPerMember();

	/** (member id, day, calories, day type) since a date, for every member in one pass. */
	@Query("""
			select i.member.id, i.consumedOn, i.calories, i.dayType from MemberIntake i where i.consumedOn >= :from
			""")
	List<Object[]> recentDays(@org.springframework.data.repository.query.Param("from") LocalDate from);

	long countByConsumedOnGreaterThanEqual(LocalDate from);
}
