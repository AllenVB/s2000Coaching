package com.s2000coaching.repository;

import com.s2000coaching.domain.WeeklyNutrition;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WeeklyNutritionRepository extends JpaRepository<WeeklyNutrition, UUID> {

	List<WeeklyNutrition> findAllByMemberIdOrderByWeekStartDesc(UUID memberId);

	Optional<WeeklyNutrition> findByMemberIdAndWeekStart(UUID memberId, LocalDate weekStart);

	void deleteAllByMemberId(UUID memberId);

	/** Newest week entered per member, in one pass — the attention panel must not query per member. */
	@Query("""
			select w.member.id, max(w.weekStart) from WeeklyNutrition w group by w.member.id
			""")
	List<Object[]> latestWeekStartPerMember();
}
