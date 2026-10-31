package com.s2000coaching.repository;

import com.s2000coaching.domain.ProgressPhoto;
import com.s2000coaching.domain.PhotoPose;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {

	List<ProgressPhoto> findAllByMemberIdOrderByTakenOnDesc(UUID memberId);

	long countByMemberId(UUID memberId);

	boolean existsByMemberIdAndPoseAndTakenOnBetween(UUID memberId, PhotoPose pose, LocalDate from, LocalDate to);

	void deleteAllByMemberId(UUID memberId);
}
