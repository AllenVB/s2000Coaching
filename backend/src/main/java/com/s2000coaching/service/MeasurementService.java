package com.s2000coaching.service;

import com.s2000coaching.domain.Measurement;
import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.web.dto.MeasurementResponse;
import com.s2000coaching.web.dto.UpsertMeasurementRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Member check-ins. The coach reads them; only the member writes them. */
@Service
public class MeasurementService {

	private final MeasurementRepository measurementRepository;
	private final MemberService memberService;

	public MeasurementService(MeasurementRepository measurementRepository, MemberService memberService) {
		this.measurementRepository = measurementRepository;
		this.memberService = memberService;
	}

	@Transactional(readOnly = true)
	public List<MeasurementResponse> listForMember(UUID memberId) {
		return measurementRepository.findAllByMemberIdOrderByMeasuredOnDesc(memberId).stream()
				.map(MeasurementResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MeasurementResponse> listForMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return listForMember(memberId);
	}

	/**
	 * Saves the check-in, replacing an existing one for the same date so a correction does not
	 * create a second row for that day.
	 */
	@Transactional
	public MeasurementResponse save(Member member, UpsertMeasurementRequest request) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Ölçüm kaydedebilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}

		Instant now = Instant.now();
		Measurement measurement = measurementRepository
				.findByMemberIdAndMeasuredOn(member.getId(), request.measuredOn())
				.orElseGet(() -> measurementRepository.save(new Measurement(
						UUID.randomUUID(), member, request.measuredOn(), request.weightKg(), now)));

		measurement.setWeightKg(request.weightKg());
		measurement.setWaistCm(request.waistCm());
		measurement.setChestCm(request.chestCm());
		measurement.setHipCm(request.hipCm());
		measurement.setArmCm(request.armCm());
		measurement.setThighCm(request.thighCm());
		measurement.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
		measurement.setUpdatedAt(now);

		return MeasurementResponse.from(measurement);
	}

	/** Members may only delete their own rows; the caller passes the resolved member. */
	@Transactional
	public void deleteOwn(Member member, UUID measurementId) {
		Measurement measurement = getOrThrow(measurementId);
		if (!measurement.getMember().getId().equals(member.getId())) {
			// Same answer as a missing row, so one member cannot probe for another's ids.
			throw new NotFoundException("Ölçüm bulunamadı: " + measurementId);
		}
		measurementRepository.delete(measurement);
	}

	@Transactional
	public void deleteAsCoach(UUID measurementId) {
		measurementRepository.delete(getOrThrow(measurementId));
	}

	private Measurement getOrThrow(UUID measurementId) {
		return measurementRepository.findById(measurementId)
				.orElseThrow(() -> new NotFoundException("Ölçüm bulunamadı: " + measurementId));
	}
}
