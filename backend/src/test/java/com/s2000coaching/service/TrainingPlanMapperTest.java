package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MuscleGroup;
import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.repository.SessionTopWeight;
import com.s2000coaching.web.dto.WeightEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The "Antrenman 1–10" strip is built from these numbers, so the numbering has to be right. */
class TrainingPlanMapperTest {

	private Member member;
	private TrainingPlanDay day;
	private TrainingPlanExercise bench;
	private TrainingPlanExercise row;

	@BeforeEach
	void setUp() {
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
		TrainingPlan plan = new TrainingPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		day = new TrainingPlanDay(UUID.randomUUID(), plan, 1, "Gün 1", null);
		plan.getDays().add(day);
		bench = exercise("Bench Press", 1);
		row = exercise("Row", 2);
	}

	private TrainingPlanExercise exercise(String name, int order) {
		Exercise exercise = new Exercise(UUID.randomUUID(), name, MuscleGroup.GOGUS, null, null, Instant.now());
		TrainingPlanExercise planExercise = new TrainingPlanExercise(UUID.randomUUID(), day, exercise, order, 3,
				"8", null, null, null);
		day.getExercises().add(planExercise);
		return planExercise;
	}

	private static SessionTopWeight session(TrainingPlanExercise of, LocalDate on, String topKg) {
		return new SessionTopWeight(of.getId(), on, topKg == null ? null : new BigDecimal(topKg));
	}

	@Test
	void shouldNumberSessionsFromOneInTheOrderGiven() {
		var histories = TrainingPlanMapper.weightHistories(List.of(
				session(bench, LocalDate.of(2026, 10, 1), "45"),
				session(bench, LocalDate.of(2026, 10, 4), "47.5")));

		assertThat(histories.get(bench.getId())).extracting(WeightEntry::sessionNumber).containsExactly(1, 2);
		assertThat(histories.get(bench.getId())).extracting(WeightEntry::topWeightKg)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("45"), new BigDecimal("47.5"));
		assertThat(histories.get(bench.getId()).get(0).performedOn()).isEqualTo(LocalDate.of(2026, 10, 1));
	}

	@Test
	void shouldNumberEachExerciseOnItsOwnSoAnotherExerciseDoesNotShiftTheCount() {
		// The row was done on the 1st only; the bench's first session is still its Antrenman 1.
		var histories = TrainingPlanMapper.weightHistories(List.of(
				session(row, LocalDate.of(2026, 10, 1), "60"),
				session(bench, LocalDate.of(2026, 10, 4), "45")));

		assertThat(histories.get(bench.getId())).singleElement()
				.satisfies(entry -> assertThat(entry.sessionNumber()).isEqualTo(1));
		assertThat(histories.get(row.getId())).hasSize(1);
	}

	@Test
	void shouldKeepASessionWithoutAWeightButLeaveItsWeightEmpty() {
		var histories = TrainingPlanMapper.weightHistories(List.of(
				session(bench, LocalDate.of(2026, 10, 1), null)));

		assertThat(histories.get(bench.getId())).singleElement().satisfies(entry -> {
			assertThat(entry.sessionNumber()).isEqualTo(1);
			assertThat(entry.topWeightKg()).isNull();
		});
	}

	@Test
	void shouldKeepTheLastTenWithTheirTrueNumbers() {
		List<SessionTopWeight> sessions = new java.util.ArrayList<>();
		for (int index = 0; index < 13; index++) {
			sessions.add(session(bench, LocalDate.of(2026, 9, 1).plusDays(index * 2L), String.valueOf(40 + index)));
		}

		var entries = TrainingPlanMapper.weightHistories(sessions).get(bench.getId());

		assertThat(entries).hasSize(10);
		assertThat(entries.get(0).sessionNumber()).isEqualTo(4);
		assertThat(entries.get(9).sessionNumber()).isEqualTo(13);
		assertThat(entries.get(9).topWeightKg()).isEqualByComparingTo("52");
	}

	@Test
	void shouldHaveNoHistoryBeforeTheFirstSession() {
		assertThat(TrainingPlanMapper.weightHistories(List.of())).isEmpty();
	}

	@Test
	void shouldAttachHistoryAndLastSessionToTheMemberResponseButNotToTheCoachOne() {
		WorkoutLog log = new WorkoutLog(UUID.randomUUID(), member, day, LocalDate.of(2026, 10, 1), Instant.now());
		log.getSets().add(new SetLog(UUID.randomUUID(), log, bench, 1, 8, new BigDecimal("45")));
		TrainingPlan plan = day.getPlan();

		var memberView = TrainingPlanMapper.toMemberResponse(plan, Map.of(day.getId(), log),
				TrainingPlanMapper.weightHistories(List.of(session(bench, LocalDate.of(2026, 10, 1), "45"))));
		var coachView = TrainingPlanMapper.toResponse(plan);

		var memberBench = memberView.days().get(0).exercises().get(0);
		assertThat(memberBench.weightHistory()).hasSize(1);
		assertThat(memberBench.lastPerformed()).hasSize(1);
		assertThat(memberView.days().get(0).lastPerformedOn()).isEqualTo(LocalDate.of(2026, 10, 1));
		assertThat(coachView.days().get(0).exercises().get(0).weightHistory()).isEmpty();
	}

	@Test
	void shouldSplitStoredSetRepsAndReportNoneForOlderExercises() {
		assertThat(TrainingPlanMapper.setRepsOf(bench)).isEmpty();

		bench.setSetReps("30|25|20");

		assertThat(TrainingPlanMapper.setRepsOf(bench)).containsExactly("30", "25", "20");
	}
}
