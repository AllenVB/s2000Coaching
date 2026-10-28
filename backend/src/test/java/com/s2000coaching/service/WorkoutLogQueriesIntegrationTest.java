package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MuscleGroup;
import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.repository.ExerciseRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.SetLogRepository;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.PlanExerciseResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Against a real PostgreSQL: that the two queries behind the member's plan return the right
 * rows, and — the reason they exist — that loading the plan costs the same number of statements
 * whether the member has done three sessions or sixty.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class WorkoutLogQueriesIntegrationTest {

	@Autowired
	private WorkoutLogService workoutLogService;
	@Autowired
	private WorkoutLogRepository workoutLogRepository;
	@Autowired
	private SetLogRepository setLogRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private ExerciseRepository exerciseRepository;
	@Autowired
	private TrainingPlanRepository planRepository;
	@Autowired
	private EntityManagerFactory entityManagerFactory;
	@Autowired
	private TransactionTemplate transactionTemplate;

	private Member member;
	private Member other;
	private TrainingPlanDay day1;
	private TrainingPlanDay day2;
	private TrainingPlanExercise bench;
	private TrainingPlanExercise row;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(newMember());
		other = memberRepository.save(newMember());
		Exercise benchExercise = exerciseRepository.save(
				new Exercise(UUID.randomUUID(), "Bench " + UUID.randomUUID(), MuscleGroup.GOGUS, null, null, Instant.now()));
		Exercise rowExercise = exerciseRepository.save(
				new Exercise(UUID.randomUUID(), "Row " + UUID.randomUUID(), MuscleGroup.SIRT, null, null, Instant.now()));

		TrainingPlan plan = new TrainingPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		day1 = new TrainingPlanDay(UUID.randomUUID(), plan, 1, "Gün 1", null);
		day2 = new TrainingPlanDay(UUID.randomUUID(), plan, 2, "Gün 2", null);
		plan.getDays().add(day1);
		plan.getDays().add(day2);
		bench = new TrainingPlanExercise(UUID.randomUUID(), day1, benchExercise, 1, 3, "8", null, null, null);
		row = new TrainingPlanExercise(UUID.randomUUID(), day2, rowExercise, 1, 3, "8", null, null, null);
		day1.getExercises().add(bench);
		day2.getExercises().add(row);
		planRepository.save(plan);
	}

	private static Member newMember() {
		return new Member(UUID.randomUUID(), UUID.randomUUID() + "@example.com", "Test Üye", "+905551112233", 180,
				new BigDecimal("80.00"), "yok", MemberStatus.ACTIVE, Instant.now());
	}

	/** One session of {@code planExercise} with three sets, the heaviest being {@code topKg}. */
	private void log(Member who, TrainingPlanDay day, TrainingPlanExercise planExercise, LocalDate on, String topKg) {
		transactionTemplate.executeWithoutResult(status -> {
			WorkoutLog log = new WorkoutLog(UUID.randomUUID(), who, day, on, Instant.now());
			for (int number = 1; number <= 3; number++) {
				BigDecimal weight = topKg == null ? null : new BigDecimal(topKg).subtract(new BigDecimal(number - 1));
				log.getSets().add(new SetLog(UUID.randomUUID(), log, planExercise, number, 8, weight));
			}
			workoutLogRepository.save(log);
		});
	}

	@Test
	void shouldGroupTheTopWeightPerSessionOldestFirstAndLeaveOtherMembersOut() {
		log(member, day1, bench, LocalDate.of(2026, 10, 4), "47.5");
		log(member, day1, bench, LocalDate.of(2026, 10, 1), "45");
		log(member, day1, bench, LocalDate.of(2026, 10, 7), null);
		log(other, day1, bench, LocalDate.of(2026, 10, 2), "100");

		var rows = setLogRepository.topWeightPerSession(member.getId(), List.of(bench.getId()));

		assertThat(rows).extracting(row -> row.performedOn())
				.containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 7));
		assertThat(rows.get(0).topWeightKg()).isEqualByComparingTo("45");
		assertThat(rows.get(1).topWeightKg()).isEqualByComparingTo("47.5");
		assertThat(rows.get(2).topWeightKg()).isNull();
	}

	@Test
	void shouldFindOnlyTheLatestSessionOfEachDayWithItsSets() {
		log(member, day1, bench, LocalDate.of(2026, 10, 1), "45");
		log(member, day1, bench, LocalDate.of(2026, 10, 4), "47.5");
		log(member, day2, row, LocalDate.of(2026, 10, 2), "60");
		log(other, day1, bench, LocalDate.of(2026, 10, 9), "100");

		var latest = workoutLogRepository.findLatestPerDay(member.getId(), List.of(day1.getId(), day2.getId()));

		assertThat(latest).extracting(WorkoutLog::getPerformedOn)
				.containsExactlyInAnyOrder(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 2));
		assertThat(latest).allSatisfy(log -> assertThat(log.getSets()).hasSize(3));
	}

	@Test
	void shouldCostTheSameNumberOfStatementsHoweverManySessionsThereAre() {
		for (int index = 0; index < 3; index++) {
			log(member, day1, bench, LocalDate.of(2026, 9, 1).plusDays(index), "45");
		}
		long few = statementsToLoadThePlan();

		for (int index = 3; index < 60; index++) {
			log(member, day1, bench, LocalDate.of(2026, 9, 1).plusDays(index), "50");
		}
		long many = statementsToLoadThePlan();

		assertThat(few).isLessThanOrEqualTo(20);
		assertThat(many).isEqualTo(few);
	}

	@Test
	void shouldNumberThePlanLoadedForTheMemberFromTheirFirstSession() {
		for (int index = 0; index < 12; index++) {
			log(member, day1, bench, LocalDate.of(2026, 9, 1).plusDays(index), String.valueOf(40 + index));
		}

		PlanExerciseResponse response = workoutLogService.findActivePlan(member.getId()).orElseThrow()
				.days().get(0).exercises().get(0);

		assertThat(response.weightHistory()).hasSize(10);
		assertThat(response.weightHistory().get(0).sessionNumber()).isEqualTo(3);
		assertThat(response.weightHistory().get(9).sessionNumber()).isEqualTo(12);
		assertThat(response.weightHistory().get(9).topWeightKg()).isEqualByComparingTo("51");
		assertThat(response.lastPerformed()).hasSize(3);
	}

	private long statementsToLoadThePlan() {
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		workoutLogService.findActivePlan(member.getId()).orElseThrow();
		return statistics.getPrepareStatementCount();
	}
}
