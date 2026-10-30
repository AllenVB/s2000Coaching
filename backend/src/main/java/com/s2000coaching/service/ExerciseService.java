package com.s2000coaching.service;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExerciseRepository;
import com.s2000coaching.repository.TrainingPlanExerciseRepository;
import com.s2000coaching.web.dto.ExerciseResponse;
import com.s2000coaching.web.dto.UpsertExerciseRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The shared exercise catalogue the coach builds plans from. */
@Service
public class ExerciseService {

	private final ExerciseRepository exerciseRepository;
	private final TrainingPlanExerciseRepository planExerciseRepository;

	public ExerciseService(ExerciseRepository exerciseRepository,
			TrainingPlanExerciseRepository planExerciseRepository) {
		this.exerciseRepository = exerciseRepository;
		this.planExerciseRepository = planExerciseRepository;
	}

	@Transactional(readOnly = true)
	public List<ExerciseResponse> list() {
		return exerciseRepository.findAllByOrderByNameAsc().stream().map(ExerciseResponse::from).toList();
	}

	@Transactional
	public ExerciseResponse create(UpsertExerciseRequest request) {
		String name = request.name().trim();
		if (exerciseRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Bu isimde bir hareket zaten var: " + name);
		}
		Exercise exercise = new Exercise(UUID.randomUUID(), name, request.muscleGroup(),
				normalizedVideoUrl(request.videoUrl()), blankToNull(request.note()), Instant.now());
		return ExerciseResponse.from(exerciseRepository.save(exercise));
	}

	@Transactional
	public ExerciseResponse update(UUID exerciseId, UpsertExerciseRequest request) {
		Exercise exercise = getOrThrow(exerciseId);
		String name = request.name().trim();
		if (!exercise.getName().equalsIgnoreCase(name) && exerciseRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Bu isimde bir hareket zaten var: " + name);
		}
		exercise.setName(name);
		exercise.setMuscleGroup(request.muscleGroup());
		exercise.setVideoUrl(normalizedVideoUrl(request.videoUrl()));
		exercise.setNote(blankToNull(request.note()));
		return ExerciseResponse.from(exercise);
	}

	/**
	 * Refuses to delete an exercise that a plan still prescribes — removing it would silently
	 * gut those plans and orphan the set history recorded against it.
	 */
	@Transactional
	public void delete(UUID exerciseId) {
		Exercise exercise = getOrThrow(exerciseId);
		if (planExerciseRepository.existsByExerciseId(exerciseId)) {
			throw new ConflictException(
					"Bu hareket en az bir planda kullanılıyor; önce planlardan çıkarın: " + exercise.getName());
		}
		exerciseRepository.delete(exercise);
	}

	Exercise requireExercise(UUID exerciseId) {
		return getOrThrow(exerciseId);
	}

	/**
	 * The exercise a plan line points at: the one with that id, or — when the coach typed a name —
	 * the catalogue entry of that name (any case), made on the spot when there is none. Typing the
	 * same name again therefore reuses the entry, and with it its video and its history.
	 */
	Exercise resolve(UUID exerciseId, String exerciseName) {
		if (exerciseId != null) {
			return getOrThrow(exerciseId);
		}
		String name = exerciseName == null ? "" : exerciseName.trim().replaceAll("\\s+", " ");
		if (name.isEmpty()) {
			throw new BadRequestException("Egzersiz adını yazın.");
		}
		return exerciseRepository.findFirstByNameIgnoreCase(name)
				.orElseGet(() -> exerciseRepository.save(new Exercise(UUID.randomUUID(), name, null, null, null,
						Instant.now())));
	}

	private Exercise getOrThrow(UUID exerciseId) {
		return exerciseRepository.findById(exerciseId)
				.orElseThrow(() -> new NotFoundException("Hareket bulunamadı: " + exerciseId));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	/**
	 * Blank clears the video; anything else must be a YouTube link and is stored in its
	 * canonical form. Refusing other hosts here is what keeps the player address something we
	 * build from a video id, not something a pasted link gets to choose.
	 */
	private static String normalizedVideoUrl(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return YouTubeLink.parse(raw)
				.map(YouTubeLink.Parsed::canonicalUrl)
				.orElseThrow(() -> new BadRequestException(
						"Video bağlantısı bir YouTube bağlantısı olmalı (örn. https://youtu.be/… ya da https://www.youtube.com/watch?v=…)."));
	}
}
