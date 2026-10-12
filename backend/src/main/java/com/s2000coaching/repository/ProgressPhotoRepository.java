package com.s2000coaching.repository;

import com.s2000coaching.domain.ProgressPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {

	List<ProgressPhoto> findAllByMemberIdOrderByTakenOnDesc(UUID memberId);

	long countByMemberId(UUID memberId);

	void deleteAllByMemberId(UUID memberId);
}
