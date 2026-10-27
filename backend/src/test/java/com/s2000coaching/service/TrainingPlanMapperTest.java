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
import com.s2000coaching.web.dto.WeightEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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

	/** A session of the day with one set of the given exercise per weight (null = no weight recorded). */
	private WorkoutLog session(LocalDate on, TrainingPlanExercise planExercise, String... weights) {
		WorkoutLog log = new WorkoutLog(UUID.randomUUID(), member, day, on, Instant.now());
		int number = 1;
		for (String weight : weights) {
			log.getSets().add(new SetLog(UUID.randomUUID(), log, planExercise, number++, 8,
					weight == null ? null : new BigDecimal(weight)));
		}
		return log;
	}

	/** The mapper is given sessions newest first, as the repository returns them. */
	private List<WeightEntry> history(TrainingPlanExercise planExercise, WorkoutLog... oldestFirst) {
		List<WorkoutLog> newestFirst = new ArrayList<>(List.of(oldestFirst));
		Collections.reverse(newestFirst);
		return TrainingPlanMapper.weightHistory(planExercise, newestFirst);
	}

	@Test
	void shouldTakeTheHeaviestSetOfEachSessionOldestFirst() {
		var entries = history(bench,
				session(LocalDate.of(2026, 10, 1), bench, "40", "45", "42.5"),
				session(LocalDate.of(2026, 10, 4), bench, "45", "47.5"));

		assertThat(entries).extracting(WeightEntry::sessionNumber).containsExactly(1, 2);
		assertThat(entries).extracting(WeightEntry::topWeightKg)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("45"), new BigDecimal("47.5"));
		assertThat(entries.get(0).performedOn()).isEqualTo(LocalDate.of(2026, 10, 1));
	}

	@Test
	void shouldSkipSessionsThatDidNotDoThisExercise() {
		var entries = history(bench,
				session(LocalDate.of(2026, 10, 1), row, "60"),
				session(LocalDate.of(2026, 10, 4), bench, "45"));

		// Antrenman 1 of the bench is the session it was first done, not the first of the day.
		assertThat(entries).singleElement().satisfies(entry -> assertThat(entry.sessionNumber()).isEqualTo(1));
	}

	@Test
	void shouldCountASessionWithoutAWeightButLeaveItsWeightEmpty() {
		var entries = history(bench, session(LocalDate.of(2026, 10, 1), bench, (String) null));

		assertThat(entries).singleElement().satisfies(entry -> {
			assertThat(entry.sessionNumber()).isEqualTo(1);
			assertThat(entry.topWeightKg()).isNull();
		});
	}

	@Test
	void shouldKeepTheLastTenWithTheirTrueNumbers() {
		WorkoutLog[] sessions = new WorkoutLog[13];
		for (int index = 0; index < sessions.length; index++) {
			sessions[index] = session(LocalDate.of(2026, 9, 1).plusDays(index * 2L), bench,
					String.valueOf(40 + index));
		}

		var entries = history(bench, sessions);

		assertThat(entries).hasSize(10);
		assertThat(entries.get(0).sessionNumber()).isEqualTo(4);
		assertThat(entries.get(9).sessionNumber()).isEqualTo(13);
		assertThat(entries.get(9).topWeightKg()).isEqualByComparingTo("52");
	}

	@Test
	void shouldBeEmptyBeforeTheFirstSession() {
		assertThat(history(bench)).isEmpty();
	}

	@Test
	void shouldAttachTheHistoryToTheMemberResponseAndNotToTheCoachOne() {
		WorkoutLog log = session(LocalDate.of(2026, 10, 1), bench, "45");
		TrainingPlan plan = day.getPlan();

		var memberView = TrainingPlanMapper.toMemberResponse(plan, Map.of(day.getId(), List.of(log)));
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
