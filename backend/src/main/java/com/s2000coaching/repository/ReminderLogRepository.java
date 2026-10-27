package com.s2000coaching.repository;

import com.s2000coaching.domain.ReminderLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, UUID> {

	boolean existsByMemberIdAndKindAndOccasion(UUID memberId, String kind, String occasion);

	void deleteAllByMemberId(UUID memberId);

	/** Everything already sent as (member id, kind, occasion), so a run checks them in memory. */
	@Query("select r.member.id, r.kind, r.occasion from ReminderLog r")
	List<Object[]> allSent();
}
