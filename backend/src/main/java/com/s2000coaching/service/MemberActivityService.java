package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberActivity;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MemberActivityRepository;
import com.s2000coaching.web.dto.ActivityResponse;
import com.s2000coaching.web.dto.UpsertActivityRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Daily steps and cardio. The member writes it; the coach reads it. */
@Service
public class MemberActivityService implements MemberDataEraser {

	private final MemberActivityRepository activityRepository;
	private final MemberService memberService;

	public MemberActivityService(MemberActivityRepository activityRepository, MemberService memberService) {
		this.activityRepository = activityRepository;
		this.memberService = memberService;
	}

	@Transactional(readOnly = true)
	public List<ActivityResponse> listForMember(UUID memberId) {
		return activityRepository.findAllByMemberIdOrderByPerformedOnDesc(memberId).stream()
				.map(ActivityResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ActivityResponse> listForMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return listForMember(memberId);
	}

	/** Replaces an existing entry for the same date, so correcting a day does not add a second row. */
	@Transactional
	public ActivityResponse save(Member member, UpsertActivityRequest request) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Adım ve kardiyo girebilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}
		if (request.steps() == null && request.cardioMinutes() == null) {
			throw new BadRequestException("Adım sayısı ya da kardiyo süresinden en az birini yazın.");
		}
		Instant now = Instant.now();
		MemberActivity activity = activityRepository
				.findByMemberIdAndPerformedOn(member.getId(), request.performedOn())
				.orElseGet(() -> activityRepository
						.save(new MemberActivity(UUID.randomUUID(), member, request.performedOn(), now)));
		activity.record(request.steps(), request.cardioMinutes(), now);
		return ActivityResponse.from(activity);
	}

	@Transactional
	public void deleteOwn(Member member, UUID activityId) {
		MemberActivity activity = activityRepository.findById(activityId)
				.orElseThrow(() -> new NotFoundException("Kayıt bulunamadı."));
		if (!activity.getMember().getId().equals(member.getId())) {
			throw new NotFoundException("Kayıt bulunamadı.");
		}
		activityRepository.delete(activity);
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		activityRepository.deleteAllByMemberId(memberId);
	}
}
