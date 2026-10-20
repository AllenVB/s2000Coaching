package com.s2000coaching.repository;

import com.s2000coaching.domain.MemberVerificationCode;
import com.s2000coaching.domain.VerificationPurpose;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberVerificationCodeRepository extends JpaRepository<MemberVerificationCode, UUID> {

	Optional<MemberVerificationCode> findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
			UUID memberId, VerificationPurpose purpose);

	List<MemberVerificationCode> findAllByMemberIdAndConsumedAtIsNull(UUID memberId);

	/** The member's last code of any purpose, consumed or not: what the cooldown measures from. */
	Optional<MemberVerificationCode> findFirstByMemberIdOrderByCreatedAtDesc(UUID memberId);

	long countByMemberIdAndCreatedAtAfter(UUID memberId, Instant createdAfter);

	void deleteAllByMemberId(UUID memberId);

	int deleteByCreatedAtBefore(Instant cutoff);
}
