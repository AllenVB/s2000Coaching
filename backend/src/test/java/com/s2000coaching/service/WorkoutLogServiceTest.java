package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MuscleGroup;
import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.SaveWorkoutLogRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutLogServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

	@Mock
	private WorkoutLogRepository workoutLogRepository;

	@Mock
	private TrainingPlanRepository planRepository;

	@Mock
	private TrainingPlanService trainingPlanService;

	private WorkoutLogService workoutLogService;
	private Member member;
	private TrainingPlan plan;
	private TrainingPlanDay day;
	private TrainingPlanExercise planExercise;

	@BeforeEach
	void setUp() {
		workoutLogService = new WorkoutLogService(workoutLogRepository, planRepository, trainingPlanService);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
		plan = new TrainingPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
		day = new TrainingPlanDay(UUID.randomUUID(), plan, 1, "Gün 1", null);
		plan.getDays().add(day);
		Exercise squat = new Exercise(UUID.randomUUID(), "Squat", MuscleGroup.BACAK, null, null, Instant.now());
		planExercise = new TrainingPlanExercise(UUID.randomUUID(), day, squat, 1, 3, "8-10",
				new BigDecimal("80.00"), 120, null);
		day.getExercises().add(planExercise);
	}

	private SaveWorkoutLogRequest request(int reps, String weight) {
		return new SaveWorkoutLogRequest(day.getId(), TODAY, "  iyi  ", List.of(
				new SaveWorkoutLogRequest.SetEntry(planExercise.getId(), 1, reps, new BigDecimal(weight))));
	}

	@Test
	void shouldStoreASession() {
		when(trainingPlanService.getDayOrThrow(day.getId())).thenReturn(day);
		when(workoutLogRepository.findByMemberIdAndDayIdAndPerformedOn(member.getId(), day.getId(), TODAY))
				.thenReturn(Optional.empty());
		when(workoutLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = workoutLogService.save(member, request(10, "60.0"));

		assertThat(response.note()).isEqualTo("iyi");
		assertThat(response.sets()).singleElement().satisfies(set -> {
			assertThat(set.reps()).isEqualTo(10);
			assertThat(set.weightKg()).isEqualByComparingTo("60.0");
		});
	}

	@Test
	void shouldReplaceTheSetsWhenTheSameDayIsSavedAgain() {
		WorkoutLog existing = new WorkoutLog(UUID.randomUUID(), member, day, TODAY, Instant.now());
		existing.getSets().add(new SetLog(UUID.randomUUID(), existing, planExercise, 1, 8,
				new BigDecimal("55.0")));
		when(trainingPlanService.getDayOrThrow(day.getId())).thenReturn(day);
		when(workoutLogRepository.findByMemberIdAndDayIdAndPerformedOn(member.getId(), day.getId(), TODAY))
				.thenReturn(Optional.of(existing));

		var response = workoutLogService.save(member, request(12, "65.0"));

		assertThat(response.id()).isEqualTo(existing.getId());
		assertThat(existing.getSets()).singleElement()
				.satisfies(set -> assertThat(set.getReps()).isEqualTo(12));
	}

	@Test
	void shouldHideAnotherMembersDayBehindA404() {
		Member other = new Member(UUID.randomUUID(), "baska@example.com", "Başka", "+905550001122", 170,
				new BigDecimal("70.00"), "Koşu", MemberStatus.ACTIVE, Instant.now());
		TrainingPlan otherPlan = new TrainingPlan(UUID.randomUUID(), other, "Plan", null, true, Instant.now());
		TrainingPlanDay otherDay = new TrainingPlanDay(UUID.randomUUID(), otherPlan, 1, "Gün 1", null);
		otherPlan.getDays().add(otherDay);
		when(trainingPlanService.getDayOrThrow(otherDay.getId())).thenReturn(otherDay);

		var request = new SaveWorkoutLogRequest(otherDay.getId(), TODAY, null, List.of());

		assertThatThrownBy(() -> workoutLogService.save(member, request))
				.isInstanceOf(NotFoundException.class)
				.hasMessageContaining("Antrenman günü bulunamadı");
	}

	@Test
	void shouldRejectASetForAnExerciseThatIsNotInThatDay() {
		when(trainingPlanService.getDayOrThrow(day.getId())).thenReturn(day);
		when(workoutLogRepository.findByMemberIdAndDayIdAndPerformedOn(member.getId(), day.getId(), TODAY))
				.thenReturn(Optional.empty());
		when(workoutLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var request = new SaveWorkoutLogRequest(day.getId(), TODAY, null, List.of(
				new SaveWorkoutLogRequest.SetEntry(UUID.randomUUID(), 1, 10, new BigDecimal("60.0"))));

		assertThatThrownBy(() -> workoutLogService.save(member, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("bu antrenman gününde yok");
	}

	@Test
	void shouldAttachTheMostRecentSessionToTheActivePlan() {
		WorkoutLog previous = new WorkoutLog(UUID.randomUUID(), member, day, TODAY.minusDays(3), Instant.now());
		previous.getSets().add(new SetLog(UUID.randomUUID(), previous, planExercise, 1, 9,
				new BigDecimal("72.5")));
		when(planRepository.findByMemberIdAndActiveTrue(member.getId())).thenReturn(Optional.of(plan));
		when(workoutLogRepository.findAllByMemberIdAndDayIdOrderByPerformedOnDesc(
				eq(member.getId()), eq(day.getId()))).thenReturn(List.of(previous));

		var response = workoutLogService.findActivePlan(member.getId()).orElseThrow();

		assertThat(response.days()).singleElement().satisfies(dayResponse -> {
			assertThat(dayResponse.lastPerformedOn()).isEqualTo(TODAY.minusDays(3));
			assertThat(dayResponse.exercises().get(0).lastPerformed()).singleElement()
					.satisfies(set -> assertThat(set.weightKg()).isEqualByComparingTo("72.5"));
		});
	}

	@Test
	void shouldReportNoPlanWhenNoneIsActive() {
		when(planRepository.findByMemberIdAndActiveTrue(member.getId())).thenReturn(Optional.empty());

		assertThat(workoutLogService.findActivePlan(member.getId())).isEmpty();
	}
}
