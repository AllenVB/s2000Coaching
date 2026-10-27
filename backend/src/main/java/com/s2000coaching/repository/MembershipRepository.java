package com.s2000coaching.repository;

import com.s2000coaching.domain.Membership;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	List<Membership> findAllByMemberIdOrderByEndsOnDesc(UUID memberId);

	boolean existsByOrderId(UUID orderId);

	void deleteAllByMemberId(UUID memberId);

	/** The end of each member's last period, in one pass for the whole roster. */
	@Query("select m.member.id, max(m.endsOn) from Membership m group by m.member.id")
	List<Object[]> latestEndPerMember();

	/** Members whose last period ends within the window; those who renewed are excluded. */
	@Query("""
			select m from Membership m
			where m.endsOn between :from and :to
			  and not exists (
			    select 1 from Membership later
			    where later.member = m.member and later.endsOn > :to
			  )
			""")
	List<Membership> findEndingBetween(LocalDate from, LocalDate to);
}
