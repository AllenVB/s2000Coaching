package com.s2000coaching.repository;

import com.s2000coaching.domain.CoachNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachNoteRepository extends JpaRepository<CoachNote, UUID> {

	List<CoachNote> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

	void deleteAllByMemberId(UUID memberId);
}
