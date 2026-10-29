package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.Membership;
import com.s2000coaching.domain.Order;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MembershipRepository;
import com.s2000coaching.web.dto.MembershipResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The periods members have paid for. Uses the repositories directly rather than {@code
 * MemberService}: that service calls into order linking, which calls here, and a cycle between
 * them would have to be broken with a lazy proxy for no gain.
 */
@Service
public class MembershipService implements MemberDataEraser {

	private final MembershipRepository membershipRepository;
	private final MemberRepository memberRepository;
	private final Clock clock;

	public MembershipService(MembershipRepository membershipRepository, MemberRepository memberRepository,
			Clock clock) {
		this.membershipRepository = membershipRepository;
		this.memberRepository = memberRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<MembershipResponse> list(UUID memberId) {
		LocalDate today = today();
		return membershipRepository.findAllByMemberIdOrderByEndsOnDesc(memberId).stream()
				.map(membership -> MembershipResponse.from(membership, today))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MembershipResponse> listAsCoach(UUID memberId) {
		requireMember(memberId);
		return list(memberId);
	}

	/**
	 * Grants the period a paid order bought. Idempotent per order, so confirming a payment
	 * twice, or linking a member to an order that already granted one, adds nothing.
	 */
	@Transactional
	public void grantFor(Order order) {
		Member member = order.getMember();
		if (member == null || membershipRepository.existsByOrderId(order.getId())) {
			return;
		}
		create(member, order, order.getCoachingPackage().getName(),
				order.getCoachingPackage().getDurationMonths());
	}

	@Transactional
	public MembershipResponse grantManually(UUID memberId, String label, int months) {
		Member member = requireMember(memberId);
		return MembershipResponse.from(create(member, null, label.trim(), months), today());
	}

	@Transactional
	public void delete(UUID membershipId) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new NotFoundException("Üyelik kaydı bulunamadı."));
		membershipRepository.delete(membership);
	}

	/**
	 * A new period begins when the previous one ends, or today if that has passed — so
	 * renewing before the end never throws away the days already paid for.
	 */
	private Membership create(Member member, Order order, String label, int months) {
		LocalDate today = today();
		LocalDate latestEnd = membershipRepository.findAllByMemberIdOrderByEndsOnDesc(member.getId()).stream()
				.map(Membership::getEndsOn)
				.findFirst()
				.orElse(null);
		LocalDate starts = latestEnd != null && latestEnd.isAfter(today) ? latestEnd : today;
		return membershipRepository.save(new Membership(UUID.randomUUID(), member, order, label, starts,
				starts.plusMonths(months), Instant.now(clock)));
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private Member requireMember(UUID memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new NotFoundException("Üye bulunamadı: " + memberId));
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		membershipRepository.deleteAllByMemberId(memberId);
	}
}
