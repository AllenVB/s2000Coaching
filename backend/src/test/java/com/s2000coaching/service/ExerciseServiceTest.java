package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.domain.MuscleGroup;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExerciseRepository;
import com.s2000coaching.repository.TrainingPlanExerciseRepository;
import com.s2000coaching.web.dto.UpsertExerciseRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

	@Mock
	private ExerciseRepository exerciseRepository;

	@Mock
	private TrainingPlanExerciseRepository planExerciseRepository;

	private ExerciseService exerciseService;

	@BeforeEach
	void setUp() {
		exerciseService = new ExerciseService(exerciseRepository, planExerciseRepository);
	}

	@Test
	void shouldTrimAndStoreANewExercise() {
		when(exerciseRepository.existsByNameIgnoreCase("Front Squat")).thenReturn(false);
		when(exerciseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = exerciseService.create(
				new UpsertExerciseRequest("  Front Squat  ", MuscleGroup.BACAK, "  ", null));

		assertThat(response.name()).isEqualTo("Front Squat");
		assertThat(response.videoUrl()).isNull();
	}

	@Test
	void shouldRejectADuplicateNameRegardlessOfCase() {
		when(exerciseRepository.existsByNameIgnoreCase("squat")).thenReturn(true);

		assertThatThrownBy(() -> exerciseService.create(
				new UpsertExerciseRequest("squat", MuscleGroup.BACAK, null, null)))
				.isInstanceOf(ConflictException.class);

		verify(exerciseRepository, never()).save(any());
	}

	@Test
	void shouldRefuseToDeleteAnExerciseAPlanStillPrescribes() {
		Exercise exercise = new Exercise(UUID.randomUUID(), "Squat", MuscleGroup.BACAK, null, null,
				Instant.now());
		when(exerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));
		when(planExerciseRepository.existsByExerciseId(exercise.getId())).thenReturn(true);

		assertThatThrownBy(() -> exerciseService.delete(exercise.getId()))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("en az bir planda kullanılıyor");

		verify(exerciseRepository, never()).delete(any());
	}

	@Test
	void shouldDeleteAnUnusedExercise() {
		Exercise exercise = new Exercise(UUID.randomUUID(), "Squat", MuscleGroup.BACAK, null, null,
				Instant.now());
		when(exerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));
		when(planExerciseRepository.existsByExerciseId(exercise.getId())).thenReturn(false);

		exerciseService.delete(exercise.getId());

		verify(exerciseRepository).delete(exercise);
	}

	@Test
	void shouldFailForAnUnknownExercise() {
		UUID unknown = UUID.randomUUID();
		when(exerciseRepository.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> exerciseService.delete(unknown)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldStoreAYouTubeLinkInItsCanonicalForm() {
		when(exerciseRepository.existsByNameIgnoreCase("Squat")).thenReturn(false);
		when(exerciseRepository.save(org.mockito.ArgumentMatchers.any()))
				.thenAnswer(call -> call.getArgument(0));

		var response = exerciseService.create(
				new UpsertExerciseRequest("Squat", MuscleGroup.BACAK, "youtu.be/dQw4w9WgXcQ?t=1m30s", null));

		assertThat(response.videoUrl()).isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=90");
		assertThat(response.video().id()).isEqualTo("dQw4w9WgXcQ");
		assertThat(response.video().startSeconds()).isEqualTo(90);
	}

	@Test
	void shouldRefuseALinkThatIsNotYouTube() {
		assertThatThrownBy(() -> exerciseService.create(
				new UpsertExerciseRequest("Squat", MuscleGroup.BACAK, "https://vimeo.com/1", null)))
				.isInstanceOf(com.s2000coaching.exception.BadRequestException.class)
				.hasMessageContaining("YouTube");
	}
}
