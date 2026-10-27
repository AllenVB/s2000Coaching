package com.s2000coaching.service;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyIntake;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.Measurement;
import com.s2000coaching.domain.MemberIntake;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberIntakeRepository;
import com.s2000coaching.service.analysis.BodyAnalysis;
import com.s2000coaching.service.analysis.BodyAnalyzer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the case-study analysis engine on a member's own check-ins and food log, for the coach.
 *
 * <p>The engine works on plain value objects, so a member's data is handed to it as in-memory
 * case-study rows that are never saved: nothing member-owned is copied into the case-study
 * tables, which is the boundary that keeps those tables safe to serve publicly.
 *
 * <p>Coach-only. A body-fat or energy-expenditure estimate with its caveats is something to
 * read with context; shown raw to the member it would be read as a verdict.
 *
 * <p>The member record has no sex, birth year or neck measurement, so the engine will say it
 * cannot estimate body composition — which is the honest answer, and it says why.
 */
@Service
public class MemberAnalysisService {

	private final MemberService memberService;
	private final MeasurementRepository measurementRepository;
	private final MemberIntakeRepository intakeRepository;
	private final Clock clock;

	public MemberAnalysisService(MemberService memberService, MeasurementRepository measurementRepository,
			MemberIntakeRepository intakeRepository, Clock clock) {
		this.memberService = memberService;
		this.measurementRepository = measurementRepository;
		this.intakeRepository = intakeRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public BodyAnalysis analyzeAsCoach(UUID memberId) {
		Member member = memberService.requireMember(memberId);

		CaseStudy shell = new CaseStudy(UUID.randomUUID(), "uye-analizi", member.getFullName(), Instant.now(clock));
		shell.setHeightCm(member.getHeightCm());

		List<CaseStudyMeasurement> measurements = measurementRepository
				.findAllByMemberIdOrderByMeasuredOnDesc(memberId).stream()
				.map(row -> toStudyMeasurement(shell, row))
				.toList();
		List<CaseStudyIntake> intakes = intakeRepository.findAllByMemberIdOrderByConsumedOnAsc(memberId).stream()
				.map(row -> toStudyIntake(shell, row))
				.toList();

		return BodyAnalyzer.analyze(shell, measurements, intakes, LocalDate.now(clock).getYear());
	}

	private static CaseStudyMeasurement toStudyMeasurement(CaseStudy shell, Measurement row) {
		var copy = new CaseStudyMeasurement(UUID.randomUUID(), shell, row.getMeasuredOn(), row.getWeightKg());
		copy.setWaistCm(row.getWaistCm());
		copy.setChestCm(row.getChestCm());
		copy.setHipCm(row.getHipCm());
		copy.setArmCm(row.getArmCm());
		copy.setThighCm(row.getThighCm());
		return copy;
	}

	private static CaseStudyIntake toStudyIntake(CaseStudy shell, MemberIntake row) {
		var copy = new CaseStudyIntake(UUID.randomUUID(), shell, row.getConsumedOn(), row.getCalories());
		copy.setProteinG(row.getProteinG());
		copy.setCarbsG(row.getCarbsG());
		copy.setFatG(row.getFatG());
		copy.setSodiumMg(row.getSodiumMg());
		return copy;
	}
}
