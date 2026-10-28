package com.s2000coaching.repository;

import com.s2000coaching.domain.SetLog;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetLogRepository extends JpaRepository<SetLog, UUID> {

	void deleteAllByPlanExerciseId(UUID planExerciseId);

	/**
	 * One row per exercise and session, oldest first, in a single query: the weight strip needs
	 * every session's top weight, and loading the sessions to find it costs one query per session.
	 */
	@Query("""
			select new com.s2000coaching.repository.SessionTopWeight(s.planExercise.id, w.performedOn, max(s.weightKg))
			from SetLog s join s.workoutLog w
			where w.member.id = :memberId and s.planExercise.id in :planExerciseIds
			group by s.planExercise.id, w.performedOn
			order by w.performedOn asc
			""")
	List<SessionTopWeight> topWeightPerSession(@Param("memberId") UUID memberId,
			@Param("planExerciseIds") Collection<UUID> planExerciseIds);
}
