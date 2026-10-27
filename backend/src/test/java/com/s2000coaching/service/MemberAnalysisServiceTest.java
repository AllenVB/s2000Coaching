package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Measurement;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberIntake;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberIntakeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAnalysisServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

	@Mock
	private MemberService memberService;

	@Mock
	private MeasurementRepository measurementRepository;

	@Mock
	private MemberIntakeRepository intakeRepository;

	private MemberAnalysisService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new MemberAnalysisService(memberService, measurementRepository, intakeRepository,
				Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC));
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("85"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	private void memberExists() {
		when(memberService.requireMember(member.getId())).thenReturn(member);
	}

	private Measurement weighIn(LocalDate day, String kg) {
		return new Measurement(UUID.randomUUID(), member, day, new BigDecimal(kg), Instant.now());
	}

	@Test
	void shouldRunTheEngineOnTheMembersCheckInsAndFoodLog() {
		memberExists();
		List<Measurement> weighIns = new ArrayList<>();
		List<MemberIntake> intakes = new ArrayList<>();
		for (int day = 0; day < 42; day++) {
			LocalDate date = TODAY.minusDays(day);
			weighIns.add(weighIn(date, "%.1f".formatted(85.0 - (41 - day) * 0.02).replace(',', '.')));
			intakes.add(new MemberIntake(UUID.randomUUID(), member, date, 2500, Instant.now()));
		}
		when(measurementRepository.findAllByMemberIdOrderByMeasuredOnDesc(member.getId())).thenReturn(weighIns);
		when(intakeRepository.findAllByMemberIdOrderByConsumedOnAsc(member.getId())).thenReturn(intakes);

		var analysis = service.analyzeAsCoach(member.getId());

		assertThat(analysis.coverage().measurementDays()).isEqualTo(42);
		assertThat(analysis.coverage().intakeDays()).isEqualTo(42);
		assertThat(analysis.tdeeWindows()).isNotEmpty();
	}

	/** No sex or neck on a member: the engine must say so, not invent a body-fat figure. */
	@Test
	void shouldNotInventABodyFatFigureWhenTheInputsAreMissing() {
		memberExists();
		when(measurementRepository.findAllByMemberIdOrderByMeasuredOnDesc(member.getId()))
				.thenReturn(List.of(weighIn(TODAY, "85.0")));
		when(intakeRepository.findAllByMemberIdOrderByConsumedOnAsc(member.getId())).thenReturn(List.of());

		var analysis = service.analyzeAsCoach(member.getId());

		assertThat(analysis.bodyComposition().bodyFatPercent()).isNull();
		assertThat(analysis.bodyComposition().note()).isNotBlank();
	}

	@Test
	void shouldCopyNothingIntoTheCaseStudyTables() {
		// The shell study and its rows are in-memory only; no repository for them is even injected.
		assertThat(MemberAnalysisService.class.getDeclaredFields())
				.extracting(java.lang.reflect.Field::getName)
				.doesNotContain("caseStudyRepository", "caseStudyMeasurementRepository", "studyRepository");
	}
}
