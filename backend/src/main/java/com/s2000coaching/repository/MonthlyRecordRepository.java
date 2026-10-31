package com.s2000coaching.repository;

import com.s2000coaching.domain.MonthlyRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyRecordRepository extends JpaRepository<MonthlyRecord, UUID> {

	/** Newest month first; "YYYY-MM" sorts the same as the calendar. */
	List<MonthlyRecord> findAllByMemberIdOrderByPeriodDesc(UUID memberId);

	Optional<MonthlyRecord> findByMemberIdAndPeriod(UUID memberId, String period);

	void deleteAllByMemberId(UUID memberId);

	/** The newest month each member filled in, in one pass for the reminder job. */
	@org.springframework.data.jpa.repository.Query("select r.member.id, max(r.period) from MonthlyRecord r group by r.member.id")
	List<Object[]> latestPeriodPerMember();
}
