package com.s2000coaching.repository;

import com.s2000coaching.domain.TrainingPlanDay;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPlanDayRepository extends JpaRepository<TrainingPlanDay, UUID> {
}
