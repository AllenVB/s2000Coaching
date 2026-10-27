package com.s2000coaching.repository;

import com.s2000coaching.domain.TrainingPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

	/** Templates are the plans with no owner. */
	List<TrainingPlan> findAllByMemberIsNullOrderByNameAsc();

	List<TrainingPlan> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

	Optional<TrainingPlan> findByMemberIdAndActiveTrue(UUID memberId);

	void deleteAllByMemberId(UUID memberId);

	/** Members who currently have a plan to follow. */
	@org.springframework.data.jpa.repository.Query("select p.member.id from TrainingPlan p where p.active = true and p.member is not null")
	List<UUID> findMemberIdsWithActivePlan();
}
