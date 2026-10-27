package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberIntake;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MemberIntakeRepository;
import com.s2000coaching.web.dto.IntakeResponse;
import com.s2000coaching.web.dto.UpsertIntakeRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What members ate, day by day. The member writes it; the coach reads it. */
@Service
public class MemberIntakeService implements MemberDataEraser {

	private final MemberIntakeRepository intakeRepository;
	private final MemberService memberService;

	public MemberIntakeService(MemberIntakeRepository intakeRepository, MemberService memberService) {
		this.intakeRepository = intakeRepository;
		this.memberService = memberService;
	}

	@Transactional(readOnly = true)
	public List<IntakeResponse> listForMember(UUID memberId) {
		return intakeRepository.findAllByMemberIdOrderByConsumedOnDesc(memberId).stream()
				.map(IntakeResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<IntakeResponse> listForMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return listForMember(memberId);
	}

	/** Replaces an existing entry for the same date, so correcting a day does not add a second row. */
	@Transactional
	public IntakeResponse save(Member member, UpsertIntakeRequest request) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Yemek kaydı girebilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}
		Instant now = Instant.now();
		MemberIntake intake = intakeRepository.findByMemberIdAndConsumedOn(member.getId(), request.consumedOn())
				.orElseGet(() -> intakeRepository.save(new MemberIntake(UUID.randomUUID(), member,
						request.consumedOn(), request.calories(), now)));
		intake.setCalories(request.calories());
		intake.setProteinG(request.proteinG());
		intake.setCarbsG(request.carbsG());
		intake.setFatG(request.fatG());
		intake.setSodiumMg(request.sodiumMg());
		intake.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
		intake.setUpdatedAt(now);
		return IntakeResponse.from(intake);
	}

	@Transactional
	public void deleteOwn(Member member, UUID intakeId) {
		MemberIntake intake = intakeRepository.findById(intakeId)
				.orElseThrow(() -> new NotFoundException("Kayıt bulunamadı."));
		if (!intake.getMember().getId().equals(member.getId())) {
			// Same answer as a missing row, so one member cannot probe for another's ids.
			throw new NotFoundException("Kayıt bulunamadı.");
		}
		intakeRepository.delete(intake);
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		intakeRepository.deleteAllByMemberId(memberId);
	}
}
