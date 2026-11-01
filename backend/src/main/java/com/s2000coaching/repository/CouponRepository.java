package com.s2000coaching.repository;

import com.s2000coaching.domain.Coupon;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

	Optional<Coupon> findByCode(String code);

	List<Coupon> findAllByOrderByCreatedAtDesc();

	/**
	 * Takes one use if any is left; the check and the increment are one statement, so two buyers cannot both
	 * take the last one. Returns 0 when the coupon was used up in the meantime.
	 */
	@Modifying
	@Query("update Coupon c set c.usedCount = c.usedCount + 1 where c.id = :id and (c.maxUses is null or c.usedCount < c.maxUses)")
	int takeOneUse(@Param("id") UUID id);
}
