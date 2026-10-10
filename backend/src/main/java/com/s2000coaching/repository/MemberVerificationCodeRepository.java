package com.s2000coaching.repository;

import com.s2000coaching.domain.MemberVerificationCode;
import com.s2000coaching.domain.VerificationPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberVerificationCodeRepository extends JpaRepository<MemberVerificationCode, UUID> {

	Optional<MemberVerificationCode> findFirstByMemberIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
			UUID memberId, VerificationPurpose purpose);

	List<MemberVerificationCode> findAllByMemberIdAndConsumedAtIsNull(UUID memberId);

	void deleteAllByMemberId(UUID memberId);
}
