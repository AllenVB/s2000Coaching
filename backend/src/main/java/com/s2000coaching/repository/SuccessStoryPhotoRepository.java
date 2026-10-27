package com.s2000coaching.repository;

import com.s2000coaching.domain.SuccessStoryPhoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuccessStoryPhotoRepository extends JpaRepository<SuccessStoryPhoto, UUID> {

	List<SuccessStoryPhoto> findAllByStoryIdOrderByDisplayOrderAsc(UUID storyId);

	Optional<SuccessStoryPhoto> findByIdAndStoryId(UUID id, UUID storyId);

	long countByStoryId(UUID storyId);

	void deleteAllByStoryId(UUID storyId);
}
