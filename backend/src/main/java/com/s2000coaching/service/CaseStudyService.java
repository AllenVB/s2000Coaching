package com.s2000coaching.service;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CaseStudyIntakeRepository;
import com.s2000coaching.repository.CaseStudyMeasurementRepository;
import com.s2000coaching.repository.CaseStudyPhotoRepository;
import com.s2000coaching.domain.CaseStudyPhoto;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.service.PhotoPairing.PeriodPhotos;
import com.s2000coaching.repository.CaseStudyRepository;
import com.s2000coaching.service.analysis.BodyAnalysis;
import com.s2000coaching.service.analysis.BodyAnalyzer;
import com.s2000coaching.web.dto.CaseStudyResponse;
import com.s2000coaching.web.dto.PublicCaseStudyResponse;
import com.s2000coaching.web.dto.PublicCaseStudySummaryResponse;
import com.s2000coaching.web.dto.UpsertCaseStudyRequest;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Case studies: the coach's own body data, and the analysis published from it. */
@Service
public class CaseStudyService {

	private static final Logger log = LoggerFactory.getLogger(CaseStudyService.class);

	private static final int MAX_SLUG_LENGTH = 120;

	private final CaseStudyRepository studyRepository;
	private final CaseStudyMeasurementRepository measurementRepository;
	private final CaseStudyIntakeRepository intakeRepository;
	private final CaseStudyPhotoRepository photoRepository;
	private final PhotoStorage photoStorage;

	public CaseStudyService(CaseStudyRepository studyRepository,
			CaseStudyMeasurementRepository measurementRepository,
			CaseStudyIntakeRepository intakeRepository,
			CaseStudyPhotoRepository photoRepository, PhotoStorage photoStorage) {
		this.photoRepository = photoRepository;
		this.photoStorage = photoStorage;
		this.studyRepository = studyRepository;
		this.measurementRepository = measurementRepository;
		this.intakeRepository = intakeRepository;
	}

	// --- coach side ---------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<CaseStudyResponse> listForCoach() {
		return studyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::describe).toList();
	}

	@Transactional(readOnly = true)
	public CaseStudyResponse readForCoach(UUID id) {
		return describe(require(id));
	}

	@Transactional
	public CaseStudyResponse create(UpsertCaseStudyRequest request) {
		Instant now = Instant.now();
		CaseStudy study = new CaseStudy(UUID.randomUUID(), uniqueSlug(request.title()),
				request.title().trim(), now);
		apply(study, request);
		return describe(studyRepository.save(study));
	}

	@Transactional
	public CaseStudyResponse update(UUID id, UpsertCaseStudyRequest request) {
		CaseStudy study = require(id);
		study.setTitle(request.title().trim());
		apply(study, request);
		study.touch(Instant.now());
		return describe(study);
	}

	/**
	 * Publishing is blocked until there is something to publish. An empty study would render
	 * a page of headings with no numbers under them, and the coach would have no way to tell
	 * from the admin list that it was the import that failed rather than the page.
	 */
	@Transactional
	public CaseStudyResponse setPublished(UUID id, boolean published) {
		CaseStudy study = require(id);
		if (published && measurementRepository.countByCaseStudyId(id) == 0) {
			throw new BadRequestException("Yayınlamak için en az bir ölçüm içe aktarılmış olmalı.");
		}
		study.setPublished(published);
		study.touch(Instant.now());
		return describe(study);
	}

	/**
	 * Switches the public photos on or off, separately from {@link #setPublished}. Turning them
	 * on needs at least one photo, and takes effect publicly only while the study is also
	 * published — the page says so, so the coach is never surprised by which switch did what.
	 */
	@Transactional
	public CaseStudyResponse setPhotosPublished(UUID id, boolean photosPublished) {
		CaseStudy study = require(id);
		if (photosPublished && photoRepository.countByCaseStudyId(id) == 0) {
			throw new BadRequestException("Yayınlamak için en az bir fotoğraf yüklenmiş olmalı.");
		}
		study.setPhotosPublished(photosPublished);
		study.touch(Instant.now());
		return describe(study);
	}

	/** Internal: switches off without the checks, for when the last photo is deleted. */
	@Transactional
	public void setPhotosPublishedUnchecked(UUID id, boolean photosPublished) {
		require(id).setPhotosPublished(photosPublished);
	}

	/** What the public page would pair up, before the coach decides to show photos. */
	@Transactional(readOnly = true)
	public List<PeriodPhotos> photoPeriodsForCoach(UUID id) {
		CaseStudy study = require(id);
		return pairPhotos(study, analyze(study));
	}

	@Transactional(readOnly = true)
	public CaseStudy requirePublished(String slug) {
		return studyRepository.findBySlugAndPublishedIsTrue(slug)
				.orElseThrow(() -> new NotFoundException("Yayınlanmış vaka çalışması bulunamadı."));
	}

	@Transactional
	public void delete(UUID id) {
		CaseStudy study = require(id);
		// Children first: the rows reference the study, so removing it alone would fail. The
		// photo files go too: a body photo must not outlive the study it was published with.
		for (CaseStudyPhoto photo : photoRepository.findAllByCaseStudyIdOrderByTakenOnAsc(id)) {
			try {
				photoStorage.delete(photo.getStorageKey());
			} catch (RuntimeException e) {
				log.warn("Vaka fotoğrafı dosyası silinemedi: {}", photo.getStorageKey(), e);
			}
		}
		photoRepository.deleteAllByCaseStudyId(id);
		intakeRepository.deleteAllByCaseStudyId(id);
		measurementRepository.deleteAllByCaseStudyId(id);
		studyRepository.delete(study);
	}

	/**
	 * Throws out one kind of imported data. The way out of a bad import — a column mapped to
	 * the wrong field, dates read month-first — where re-importing cannot help, because the
	 * wrong rows carry dates the corrected file does not mention and so would survive the
	 * upsert.
	 */
	@Transactional
	public void clearData(UUID id, boolean measurements) {
		require(id);
		if (measurements) {
			measurementRepository.deleteAllByCaseStudyId(id);
		} else {
			intakeRepository.deleteAllByCaseStudyId(id);
		}
	}

	/** The analysis as the coach sees it before publishing — identical to the public one. */
	@Transactional(readOnly = true)
	public BodyAnalysis analyzeForCoach(UUID id) {
		return analyze(require(id));
	}

	@Transactional(readOnly = true)
	public CaseStudy require(UUID id) {
		return studyRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Vaka çalışması bulunamadı."));
	}

	@Transactional
	public void touch(UUID id) {
		require(id).touch(Instant.now());
	}

	// --- public side --------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<PublicCaseStudySummaryResponse> listPublished() {
		List<PublicCaseStudySummaryResponse> summaries = new ArrayList<>();
		for (CaseStudy study : studyRepository.findAllByPublishedIsTrueOrderByCreatedAtDesc()) {
			List<CaseStudyMeasurement> measurements =
					measurementRepository.findAllByCaseStudyIdOrderByMeasuredOnAsc(study.getId());
			summaries.add(PublicCaseStudySummaryResponse.from(study,
					measurements.isEmpty() ? null : measurements.get(0).getMeasuredOn(),
					measurements.isEmpty() ? null
							: measurements.get(measurements.size() - 1).getMeasuredOn(),
					measurements.size(),
					(int) intakeRepository.countByCaseStudyId(study.getId())));
		}
		return summaries;
	}

	/**
	 * Looked up by slug <em>and</em> published flag in one query, so an unpublished draft
	 * cannot be read by guessing its slug.
	 */
	@Transactional(readOnly = true)
	public PublicCaseStudyResponse readPublished(String slug) {
		CaseStudy study = requirePublished(slug);
		BodyAnalysis analysis = analyze(study);
		return PublicCaseStudyResponse.from(study, analysis,
				study.isPhotosPublished() ? pairPhotos(study, analysis) : null);
	}

	// --- internals ----------------------------------------------------------------------

	/**
	 * Computed per request rather than stored. The rows number in the hundreds, so the cost
	 * is trivial next to the cost of a cached analysis that no longer matches the data behind
	 * it — which is exactly the kind of wrongness nobody notices.
	 */
	private BodyAnalysis analyze(CaseStudy study) {
		return BodyAnalyzer.analyze(study,
				measurementRepository.findAllByCaseStudyIdOrderByMeasuredOnAsc(study.getId()),
				intakeRepository.findAllByCaseStudyIdOrderByConsumedOnAsc(study.getId()),
				LocalDate.now().getYear());
	}

	private List<PeriodPhotos> pairPhotos(CaseStudy study, BodyAnalysis analysis) {
		return PhotoPairing.pair(analysis.longTermPeriods(),
				photoRepository.findAllByCaseStudyIdOrderByTakenOnAsc(study.getId()));
	}

	private void apply(CaseStudy study, UpsertCaseStudyRequest request) {
		study.setSummary(request.summary());
		study.setHeightCm(request.heightCm());
		study.setSex(request.sex());
		study.setBirthYear(request.birthYear());
	}

	private CaseStudyResponse describe(CaseStudy study) {
		return CaseStudyResponse.from(study, measurementRepository.countByCaseStudyId(study.getId()),
				intakeRepository.countByCaseStudyId(study.getId()),
				photoRepository.countByCaseStudyId(study.getId()));
	}

	private String uniqueSlug(String title) {
		String base = slugify(title);
		if (!studyRepository.existsBySlug(base)) {
			return base;
		}
		// Two studies may legitimately share a title; the URL cannot.
		for (int suffix = 2; suffix < 100; suffix++) {
			String candidate = trim(base, MAX_SLUG_LENGTH - 4) + "-" + suffix;
			if (!studyRepository.existsBySlug(candidate)) {
				return candidate;
			}
		}
		throw new BadRequestException("Bu başlıktan benzersiz bir adres üretilemedi; başlığı değiştirin.");
	}

	/**
	 * Turkish letters are folded by hand before the accent-stripping pass, because
	 * {@code ı} and {@code ş} do not decompose into an ASCII letter plus a mark the way
	 * {@code ü} and {@code ö} do — left to the normalizer they would simply be dropped,
	 * turning "ışık" into "k".
	 */
	static String slugify(String title) {
		String folded = title.toLowerCase(Locale.forLanguageTag("tr-TR"))
				.replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
				.replace('ü', 'u').replace('ö', 'o').replace('ç', 'c');
		String ascii = Normalizer.normalize(folded, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

		String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
		return slug.isEmpty() ? "vaka" : trim(slug, MAX_SLUG_LENGTH);
	}

	private static String trim(String value, int max) {
		String trimmed = value.length() <= max ? value : value.substring(0, max);
		return trimmed.replaceAll("-+$", "");
	}
}
