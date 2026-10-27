package com.s2000coaching.service;

import com.s2000coaching.domain.CheckIn;
import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.web.dto.CheckInResponse;
import com.s2000coaching.web.dto.UpsertCheckInRequest;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Weekly check-ins: the member fills one in, the coach answers it. */
@Service
public class CheckInService implements MemberDataEraser {

	private final CheckInRepository checkInRepository;
	private final MemberService memberService;
	private final Clock clock;

	public CheckInService(CheckInRepository checkInRepository, MemberService memberService, Clock clock) {
		this.checkInRepository = checkInRepository;
		this.memberService = memberService;
		this.clock = clock;
	}

	/** The Monday of the week containing {@code day}. */
	static LocalDate weekStart(LocalDate day) {
		return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	@Transactional(readOnly = true)
	public List<CheckInResponse> listForMember(UUID memberId) {
		return checkInRepository.findAllByMemberIdOrderByWeekStartDesc(memberId).stream()
				.map(CheckInResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CheckInResponse> listForMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return listForMember(memberId);
	}

	/** Submitting a week again replaces it, and an earlier coach reply no longer applies. */
	@Transactional
	public CheckInResponse save(Member member, UpsertCheckInRequest request) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Haftalık değerlendirme girebilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}
		LocalDate week = weekStart(request.weekOf());
		Instant now = Instant.now(clock);
		CheckIn checkIn = checkInRepository.findByMemberIdAndWeekStart(member.getId(), week)
				.orElseGet(() -> checkInRepository.save(new CheckIn(UUID.randomUUID(), member, week, now)));
		checkIn.fill(request.sleep(), request.stress(), request.energy(), request.hunger(),
				request.dietAdherencePercent(), blankToNull(request.note()), now);
		return CheckInResponse.from(checkIn);
	}

	@Transactional
	public CheckInResponse reply(UUID checkInId, String reply) {
		CheckIn checkIn = checkInRepository.findById(checkInId)
				.orElseThrow(() -> new NotFoundException("Değerlendirme bulunamadı."));
		checkIn.reply(reply.trim(), Instant.now(clock));
		return CheckInResponse.from(checkIn);
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		checkInRepository.deleteAllByMemberId(memberId);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
