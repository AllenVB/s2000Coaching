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

	@Mock
	private com.s2000coaching.storage.PhotoStorage photoStorage;

	private ExerciseService exerciseService;

	@BeforeEach
	void setUp() {
		exerciseService = new ExerciseService(exerciseRepository, planExerciseRepository, photoStorage, 8L * 1024 * 1024);
	}

	private static byte[] png() {
		try {
			var image = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
			var out = new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private com.s2000coaching.domain.Exercise existing() {
		var exercise = new com.s2000coaching.domain.Exercise(java.util.UUID.randomUUID(), "Squat", MuscleGroup.BACAK, null,
				null, java.time.Instant.now());
		when(exerciseRepository.findById(exercise.getId())).thenReturn(java.util.Optional.of(exercise));
		return exercise;
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

	@Test
	void shouldReuseTheCatalogueEntryOfATypedNameInAnyCase() {
		var existing = new com.s2000coaching.domain.Exercise(java.util.UUID.randomUUID(), "Bench Press",
				MuscleGroup.GOGUS, null, null, java.time.Instant.now());
		when(exerciseRepository.findFirstByNameIgnoreCase("bench press")).thenReturn(java.util.Optional.of(existing));

		var resolved = exerciseService.resolve(null, "  bench   press ");

		assertThat(resolved).isSameAs(existing);
		verify(exerciseRepository, never()).save(any());
	}

	@Test
	void shouldCreateAnEntryWithoutAMuscleGroupForANewTypedName() {
		when(exerciseRepository.findFirstByNameIgnoreCase("Cable Fly")).thenReturn(java.util.Optional.empty());
		when(exerciseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var created = exerciseService.resolve(null, " Cable Fly ");

		assertThat(created.getName()).isEqualTo("Cable Fly");
		assertThat(created.getMuscleGroup()).isNull();
		verify(exerciseRepository).save(created);
	}

	@Test
	void shouldRefuseAnEmptyTypedName() {
		assertThatThrownBy(() -> exerciseService.resolve(null, "   "))
				.isInstanceOf(com.s2000coaching.exception.BadRequestException.class);
		assertThatThrownBy(() -> exerciseService.resolve(null, null))
				.isInstanceOf(com.s2000coaching.exception.BadRequestException.class);
	}

	@Test
	void shouldStoreAReEncodedImageAndKeepOnlyItsKey() {
		var exercise = existing();
		when(photoStorage.store(any(), org.mockito.ArgumentMatchers.eq("image/png"))).thenReturn("key-1");

		var response = exerciseService.setImage(exercise.getId(), png());

		assertThat(response.hasImage()).isTrue();
		assertThat(response.imageUpdatedAt()).isNotNull();
		assertThat(exercise.getImageKey()).isEqualTo("key-1");
		assertThat(exercise.getImageContentType()).isEqualTo("image/png");
	}

	@Test
	void shouldDeleteTheOldFileWhenTheImageIsReplacedOrRemoved() {
		var exercise = existing();
		exercise.attachImage("old-key", "image/png", 10, java.time.Instant.now());
		when(photoStorage.store(any(), any())).thenReturn("new-key");

		exerciseService.setImage(exercise.getId(), png());
		verify(photoStorage).delete("old-key");

		exerciseService.removeImage(exercise.getId());
		assertThat(exercise.hasImage()).isFalse();
		verify(photoStorage).delete("new-key");
	}

	@Test
	void shouldRefuseAnUploadThatIsNotAnImageOrIsEmptyOrTooBig() {
		var exercise = existing();

		assertThatThrownBy(() -> exerciseService.setImage(exercise.getId(), "<svg onload=alert(1)>".getBytes()))
				.isInstanceOf(com.s2000coaching.exception.BadRequestException.class);
		assertThatThrownBy(() -> exerciseService.setImage(exercise.getId(), new byte[0]))
				.isInstanceOf(com.s2000coaching.exception.BadRequestException.class);
		var small = new ExerciseService(exerciseRepository, planExerciseRepository, photoStorage, 10);
		assertThatThrownBy(() -> small.setImage(exercise.getId(), png()))
				.isInstanceOf(com.s2000coaching.exception.BadRequestException.class);
		verify(photoStorage, never()).store(any(), any());
		assertThat(exercise.hasImage()).isFalse();
	}

	@Test
	void shouldAnswer404ForAnExerciseWithoutAnImage() {
		var exercise = existing();

		assertThatThrownBy(() -> exerciseService.readImage(exercise.getId()))
				.isInstanceOf(com.s2000coaching.exception.NotFoundException.class);
	}

	@Test
	void shouldRemoveTheImageFileWhenTheExerciseIsDeleted() {
		var exercise = existing();
		exercise.attachImage("img-key", "image/png", 10, java.time.Instant.now());
		when(planExerciseRepository.existsByExerciseId(exercise.getId())).thenReturn(false);

		exerciseService.delete(exercise.getId());

		verify(exerciseRepository).delete(exercise);
		verify(photoStorage).delete("img-key");
	}
}
