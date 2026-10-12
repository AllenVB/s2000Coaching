package com.s2000coaching.repository;

import com.s2000coaching.domain.NutritionPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, UUID> {

	List<NutritionPlan> findAllByMemberIsNullOrderByNameAsc();

	List<NutritionPlan> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

	Optional<NutritionPlan> findByMemberIdAndActiveTrue(UUID memberId);

	void deleteAllByMemberId(UUID memberId);
}
