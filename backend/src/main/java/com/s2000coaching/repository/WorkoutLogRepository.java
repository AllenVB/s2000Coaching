package com.s2000coaching.repository;

import com.s2000coaching.domain.WorkoutLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {

	Optional<WorkoutLog> findByMemberIdAndDayIdAndPerformedOn(UUID memberId, UUID dayId, LocalDate performedOn);

	List<WorkoutLog> findAllByMemberIdOrderByPerformedOnDesc(UUID memberId);

	/** Newest first, so the first hit is the session to compare against. */
	List<WorkoutLog> findAllByMemberIdAndDayIdOrderByPerformedOnDesc(UUID memberId, UUID dayId);

	void deleteAllByMemberId(UUID memberId);

	/** Logs point at plan days, so they must go before the days that own them. */
	void deleteAllByDayIdIn(List<UUID> dayIds);
}
