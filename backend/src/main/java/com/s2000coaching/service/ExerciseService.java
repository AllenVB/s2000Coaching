package com.s2000coaching.service;

import com.s2000coaching.domain.Exercise;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ExerciseRepository;
import com.s2000coaching.repository.TrainingPlanExerciseRepository;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.web.dto.ExerciseResponse;
import com.s2000coaching.web.dto.UpsertExerciseRequest;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The shared exercise catalogue the coach builds plans from. */
@Service
public class ExerciseService {

	private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

	private final ExerciseRepository exerciseRepository;
	private final TrainingPlanExerciseRepository planExerciseRepository;
	private final PhotoStorage photoStorage;
	private final long maxImageBytes;

	public ExerciseService(ExerciseRepository exerciseRepository,
			TrainingPlanExerciseRepository planExerciseRepository,
			PhotoStorage photoStorage,
			@Value("${app.photos.max-size-bytes}") long maxImageBytes) {
		this.exerciseRepository = exerciseRepository;
		this.planExerciseRepository = planExerciseRepository;
		this.photoStorage = photoStorage;
		this.maxImageBytes = maxImageBytes;
	}

	/** The bytes plus what the browser needs to render them. */
	public record StoredImage(InputStream content, String contentType, long sizeBytes) {
	}

	/**
	 * Replaces the exercise's demonstration image. The upload is decoded and written back out as a bare
	 * image ({@link PhotoSanitizer}), which drops any metadata and proves the bytes really are a picture;
	 * members are shown it, so it must not carry what a phone screenshot or camera adds.
	 */
	@Transactional
	public ExerciseResponse setImage(UUID exerciseId, byte[] upload) {
		Exercise exercise = getOrThrow(exerciseId);
		if (upload == null || upload.length == 0) {
			throw new BadRequestException("Görsel dosyası boş.");
		}
		if (upload.length > maxImageBytes) {
			throw new BadRequestException("Görsel en fazla %d MB olabilir.".formatted(maxImageBytes / (1024 * 1024)));
		}
		PhotoSanitizer.Sanitized clean = PhotoSanitizer.sanitize(upload);
		String oldKey = exercise.getImageKey();
		// Bytes first, then the row: a row pointing at nothing is a broken picture, bytes with no row are invisible.
		String key = photoStorage.store(clean.content(), clean.contentType());
		exercise.attachImage(key, clean.contentType(), clean.content().length, Instant.now());
		if (oldKey != null) {
			deleteFileAfterCommit(oldKey);
		}
		return ExerciseResponse.from(exercise);
	}

	@Transactional
	public ExerciseResponse removeImage(UUID exerciseId) {
		Exercise exercise = getOrThrow(exerciseId);
		String oldKey = exercise.getImageKey();
		exercise.clearImage();
		if (oldKey != null) {
			deleteFileAfterCommit(oldKey);
		}
		return ExerciseResponse.from(exercise);
	}

	@Transactional(readOnly = true)
	public StoredImage readImage(UUID exerciseId) {
		Exercise exercise = getOrThrow(exerciseId);
		if (!exercise.hasImage()) {
			throw new NotFoundException("Bu hareketin görseli yok.");
		}
		return new StoredImage(photoStorage.read(exercise.getImageKey()), exercise.getImageContentType(),
				exercise.getImageSizeBytes());
	}

	private void deleteFileAfterCommit(String key) {
		AfterCommit.run(() -> {
			try {
				photoStorage.delete(key);
			} catch (RuntimeException e) {
				log.warn("Hareket görseli silinemedi: {}", key, e);
			}
		});
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
		String imageKey = exercise.getImageKey();
		exerciseRepository.delete(exercise);
		if (imageKey != null) {
			deleteFileAfterCommit(imageKey);
		}
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
