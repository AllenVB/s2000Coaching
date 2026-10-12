package com.s2000coaching.repository;

import com.s2000coaching.domain.SetLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetLogRepository extends JpaRepository<SetLog, UUID> {

	void deleteAllByPlanExerciseId(UUID planExerciseId);
}
