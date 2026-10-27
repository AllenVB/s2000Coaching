package com.s2000coaching.repository;

import com.s2000coaching.domain.FormCheck;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FormCheckRepository extends JpaRepository<FormCheck, UUID> {

	List<FormCheck> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

	void deleteAllByMemberId(UUID memberId);

	@Query("select distinct f.member.id from FormCheck f where f.coachReply is null")
	List<UUID> findMemberIdsWithUnansweredFormCheck();
}
