package com.s2000coaching.repository;

import com.s2000coaching.domain.StoryStatus;
import com.s2000coaching.domain.SuccessStory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuccessStoryRepository extends JpaRepository<SuccessStory, UUID> {

	List<SuccessStory> findAllByOrderByCreatedAtDesc();

	/** The member's stories the member has been asked about — never drafts. */
	List<SuccessStory> findAllByMemberIdAndStatusNotOrderByCreatedAtDesc(UUID memberId, StoryStatus excluded);

	List<SuccessStory> findAllByMemberId(UUID memberId);

	List<SuccessStory> findAllByStatusOrderByPublishedAtDesc(StoryStatus status);

	/** One query for id and status together: a story that is not published is simply not there. */
	Optional<SuccessStory> findByIdAndStatus(UUID id, StoryStatus status);

	boolean existsByMemberIdAndStatus(UUID memberId, StoryStatus status);
}
