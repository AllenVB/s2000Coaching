package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MuscleGroup;
import com.s2000coaching.domain.TrainingPlan;
import com.s2000coaching.domain.TrainingPlanDay;
import com.s2000coaching.domain.TrainingPlanExercise;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.SetLogRepository;
import com.s2000coaching.repository.TrainingPlanDayRepository;
import com.s2000coaching.repository.TrainingPlanExerciseRepository;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.UpsertPlanDayRequest;
import com.s2000coaching.web.dto.UpsertPlanExerciseRequest;
import com.s2000coaching.web.dto.UpsertTrainingPlanRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

	@Mock
	private TrainingPlanRepository planRepository;

	@Mock
	private TrainingPlanDayRepository dayRepository;

	@Mock
	private TrainingPlanExerciseRepository planExerciseRepository;

	@Mock
	private WorkoutLogRepository workoutLogRepository;

	@Mock
	private SetLogRepository setLogRepository;

	@Mock
	private ExerciseService exerciseService;

	@Mock
	private MemberService memberService;

	private TrainingPlanService trainingPlanService;
	private Member member;
	private Exercise squat;

	@BeforeEach
	void setUp() {
		trainingPlanService = new TrainingPlanService(planRepository, dayRepository, planExerciseRepository,
				workoutLogRepository, setLogRepository, exerciseService, memberService);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
		squat = new Exercise(UUID.randomUUID(), "Squat", MuscleGroup.BACAK, null, null, Instant.now());
	}

	/** A template with one day holding one exercise. */
	private TrainingPlan templateWithOneExercise() {
		TrainingPlan template = new TrainingPlan(UUID.randomUUID(), null, "PPL", null, false, Instant.now());
		TrainingPlanDay day = new TrainingPlanDay(UUID.randomUUID(), template, 1, "Gün 1", "notu");
		template.getDays().add(day);
		day.getExercises().add(new TrainingPlanExercise(UUID.randomUUID(), day, squat, 1, 4, "6-8",
				new BigDecimal("80.00"), 120, null));
		return template;
	}

	@Nested
	class AssigningATemplate {

		@Test
		void shouldDeepCopyTheTemplateAndActivateTheCopy() {
			TrainingPlan template = templateWithOneExercise();
			when(memberService.requireMember(member.getId())).thenReturn(member);
			when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(planRepository.findById(template.getId())).thenReturn(Optional.of(template));
			when(planRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())).thenReturn(List.of());

			var response = trainingPlanService.createForMember(member.getId(),
					new UpsertTrainingPlanRequest("Ekim Bloğu", null), template.getId());

			assertThat(response.active()).isTrue();
			assertThat(response.template()).isFalse();
			assertThat(response.days()).singleElement().satisfies(day -> {
				assertThat(day.name()).isEqualTo("Gün 1");
				assertThat(day.exercises()).singleElement().satisfies(exercise -> {
					assertThat(exercise.exercise().name()).isEqualTo("Squat");
					assertThat(exercise.setCount()).isEqualTo(4);
					// A copy, not a reference: the ids must differ from the template's.
					assertThat(exercise.id()).isNotEqualTo(template.getDays().get(0).getExercises().get(0).getId());
				});
				assertThat(day.id()).isNotEqualTo(template.getDays().get(0).getId());
			});
		}

		@Test
		void shouldRefuseToCopySomethingThatIsNotATemplate() {
			TrainingPlan otherMembersPlan = new TrainingPlan(UUID.randomUUID(), member, "Başkasının planı",
					null, true, Instant.now());
			when(memberService.requireMember(member.getId())).thenReturn(member);
			when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
			when(planRepository.findById(otherMembersPlan.getId())).thenReturn(Optional.of(otherMembersPlan));

			assertThatThrownBy(() -> trainingPlanService.createForMember(member.getId(),
					new UpsertTrainingPlanRequest("Kopya", null), otherMembersPlan.getId()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Yalnızca şablonlar");
		}
	}

	@Nested
	class Activation {

		@Test
		void shouldLeaveOnlyOnePlanActive() {
			TrainingPlan older = new TrainingPlan(UUID.randomUUID(), member, "Eski", null, true, Instant.now());
			TrainingPlan newer = new TrainingPlan(UUID.randomUUID(), member, "Yeni", null, false, Instant.now());
			when(planRepository.findById(newer.getId())).thenReturn(Optional.of(newer));
			when(planRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId()))
					.thenReturn(List.of(newer, older));

			trainingPlanService.activatePlan(newer.getId());

			assertThat(newer.isActive()).isTrue();
			assertThat(older.isActive()).isFalse();
		}

		@Test
		void shouldRefuseToActivateATemplate() {
			TrainingPlan template = templateWithOneExercise();
			when(planRepository.findById(template.getId())).thenReturn(Optional.of(template));

			assertThatThrownBy(() -> trainingPlanService.activatePlan(template.getId()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Şablonlar aktifleştirilemez");
		}
	}

	@Nested
	class Editing {

		@Test
		void shouldNumberDaysAndExercisesInOrder() {
			TrainingPlan plan = new TrainingPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
			when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

			trainingPlanService.addDay(plan.getId(), new UpsertPlanDayRequest("Gün 1", null));
			trainingPlanService.addDay(plan.getId(), new UpsertPlanDayRequest("Gün 2", null));

			assertThat(plan.getDays()).extracting(TrainingPlanDay::getDayOrder).containsExactly(1, 2);

			TrainingPlanDay first = plan.getDays().get(0);
			when(dayRepository.findById(first.getId())).thenReturn(Optional.of(first));
			when(exerciseService.requireExercise(squat.getId())).thenReturn(squat);

			trainingPlanService.addExercise(first.getId(),
					new UpsertPlanExerciseRequest(squat.getId(), 3, " 8-10 ", null, null, null));

			assertThat(first.getExercises()).singleElement().satisfies(exercise -> {
				assertThat(exercise.getDisplayOrder()).isEqualTo(1);
				assertThat(exercise.getReps()).isEqualTo("8-10");
			});
		}

		@Test
		void shouldRemoveRecordedSessionsBeforeDeletingThePlanTheyPointAt() {
			TrainingPlan plan = new TrainingPlan(UUID.randomUUID(), member, "Plan", null, true, Instant.now());
			TrainingPlanDay day = new TrainingPlanDay(UUID.randomUUID(), plan, 1, "Gün 1", null);
			plan.getDays().add(day);
			when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

			trainingPlanService.deletePlan(plan.getId());

			verify(workoutLogRepository).deleteAllByDayIdIn(List.of(day.getId()));
			verify(planRepository).delete(plan);
		}

		@Test
		void shouldNotTouchLogsWhenThePlanHasNoDays() {
			TrainingPlan plan = new TrainingPlan(UUID.randomUUID(), member, "Boş", null, true, Instant.now());
			when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

			trainingPlanService.deletePlan(plan.getId());

			verify(workoutLogRepository, never()).deleteAllByDayIdIn(any());
			verify(planRepository).delete(plan);
		}
	}
}
