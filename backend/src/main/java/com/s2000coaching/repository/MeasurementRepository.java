package com.s2000coaching.repository;

import com.s2000coaching.domain.Measurement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {

	List<Measurement> findAllByMemberIdOrderByMeasuredOnDesc(UUID memberId);

	Optional<Measurement> findByMemberIdAndMeasuredOn(UUID memberId, LocalDate measuredOn);

	void deleteAllByMemberId(UUID memberId);

	/** Active members whose newest check-in is older than {@code cutoff}, or who have none. */
	@Query("""
			select m.id from Member m
			where m.status = com.s2000coaching.domain.MemberStatus.ACTIVE
			  and not exists (
			    select 1 from Measurement x
			    where x.member = m and x.measuredOn >= :cutoff
			  )
			""")
	List<UUID> findActiveMemberIdsWithoutMeasurementSince(LocalDate cutoff);

	@Query("select m.member.id, max(m.measuredOn) from Measurement m group by m.member.id")
	List<Object[]> latestMeasuredOnPerMember();
}
