package com.s2000coaching.service;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyPhoto;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CaseStudyPhotoRepository;
import com.s2000coaching.service.ProgressPhotoService.StoredPhoto;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.web.dto.CaseStudyPhotoResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos of the coach's own body for a case study.
 *
 * <p>Only the coach uploads, and every upload is re-encoded by {@link PhotoSanitizer} before
 * it is stored: these files can end up public, so the bytes on disk are the clean ones — there
 * is no later step where the original, GPS and all, could be served by mistake.
 *
 * <p>The public read is allowed only while the study is published <em>and</em> its photos are
 * switched on, looked up in one query by slug so a draft cannot be reached by guessing.
 */
@Service
public class CaseStudyPhotoService {

	private static final Logger log = LoggerFactory.getLogger(CaseStudyPhotoService.class);

	private final CaseStudyPhotoRepository photoRepository;
	private final PhotoStorage photoStorage;
	private final CaseStudyService caseStudyService;
	private final long maxBytes;
	private final int maxPerStudy;

	public CaseStudyPhotoService(CaseStudyPhotoRepository photoRepository, PhotoStorage photoStorage,
			CaseStudyService caseStudyService,
			@Value("${app.photos.max-size-bytes}") long maxBytes,
			@Value("${app.photos.max-per-member}") int maxPerStudy) {
		this.photoRepository = photoRepository;
		this.photoStorage = photoStorage;
		this.caseStudyService = caseStudyService;
		this.maxBytes = maxBytes;
		this.maxPerStudy = maxPerStudy;
	}

	@Transactional(readOnly = true)
	public List<CaseStudyPhotoResponse> list(UUID studyId) {
		caseStudyService.require(studyId);
		return photoRepository.findAllByCaseStudyIdOrderByTakenOnAsc(studyId).stream()
				.map(CaseStudyPhotoResponse::from)
				.toList();
	}

	@Transactional
	public CaseStudyPhotoResponse upload(UUID studyId, LocalDate takenOn, PhotoPose pose, byte[] content) {
		caseStudyService.require(studyId);
		if (takenOn.isAfter(LocalDate.now())) {
			throw new BadRequestException("Çekim tarihi gelecekte olamaz.");
		}
		if (content.length == 0) {
			throw new BadRequestException("Fotoğraf dosyası boş.");
		}
		if (content.length > maxBytes) {
			throw new BadRequestException(
					"Fotoğraf en fazla %d MB olabilir.".formatted(maxBytes / (1024 * 1024)));
		}
		if (photoRepository.countByCaseStudyId(studyId) >= maxPerStudy) {
			throw new BadRequestException(
					"En fazla %d fotoğraf saklanabilir. Yeni yüklemek için eskilerden silin."
							.formatted(maxPerStudy));
		}

		PhotoSanitizer.Sanitized clean = PhotoSanitizer.sanitize(content);
		// Bytes first, row second: bytes with no row are invisible and reclaimable, a row
		// pointing at nothing is a broken image on a public page.
		String storageKey = photoStorage.store(clean.content(), clean.contentType());
		CaseStudyPhoto photo = photoRepository.save(new CaseStudyPhoto(UUID.randomUUID(), studyId, takenOn,
				pose, storageKey, clean.contentType(), clean.content().length, Instant.now()));
		return CaseStudyPhotoResponse.from(photo);
	}

	@Transactional
	public void delete(UUID photoId) {
		CaseStudyPhoto photo = photoRepository.findById(photoId)
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı."));
		try {
			photoStorage.delete(photo.getStorageKey());
		} catch (RuntimeException e) {
			log.warn("Vaka fotoğrafı dosyası silinemedi, kayıt yine de siliniyor: {}", photo.getStorageKey(), e);
		}
		photoRepository.delete(photo);
		// The last photo going away must also switch the public photos off, or the flag would
		// stay on with nothing behind it and come back silently with the next upload.
		if (photoRepository.countByCaseStudyId(photo.getCaseStudyId()) == 0) {
			caseStudyService.setPhotosPublishedUnchecked(photo.getCaseStudyId(), false);
		}
	}

	@Transactional(readOnly = true)
	public StoredPhoto readAsCoach(UUID photoId) {
		return read(photoRepository.findById(photoId)
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı.")));
	}

	/** The public read: published study, photos switched on, and the photo belongs to it. */
	@Transactional(readOnly = true)
	public StoredPhoto readPublic(String slug, UUID photoId) {
		CaseStudy study = caseStudyService.requirePublished(slug);
		if (!study.isPhotosPublished()) {
			throw new NotFoundException("Fotoğraf bulunamadı.");
		}
		return read(photoRepository.findByIdAndCaseStudyId(photoId, study.getId())
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı.")));
	}

	private StoredPhoto read(CaseStudyPhoto photo) {
		return new StoredPhoto(photoStorage.read(photo.getStorageKey()), photo.getContentType(),
				photo.getSizeBytes());
	}
}
