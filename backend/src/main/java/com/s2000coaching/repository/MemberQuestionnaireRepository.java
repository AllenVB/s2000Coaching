package com.s2000coaching.repository;

import com.s2000coaching.domain.MemberQuestionnaire;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberQuestionnaireRepository extends JpaRepository<MemberQuestionnaire, UUID> {

	Optional<MemberQuestionnaire> findByMemberId(UUID memberId);

	void deleteAllByMemberId(UUID memberId);

	/** Members who answered yes to at least one readiness question. */
	@Query("select q.member.id from MemberQuestionnaire q where q.parqAnswers like '%Y%'")
	List<UUID> findMemberIdsNeedingClearance();

	@Query("select q.member.id from MemberQuestionnaire q")
	List<UUID> findMemberIdsWithQuestionnaire();
}
