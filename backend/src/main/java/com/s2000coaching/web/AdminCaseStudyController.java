package com.s2000coaching.web;

import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.service.CaseStudyImportService;
import com.s2000coaching.service.CaseStudyService;
import com.s2000coaching.service.analysis.BodyAnalysis;
import com.s2000coaching.web.dto.CaseStudyResponse;
import com.s2000coaching.web.dto.CsvPreviewResponse;
import com.s2000coaching.web.dto.ImportResultResponse;
import com.s2000coaching.web.dto.UpsertCaseStudyRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.service.CaseStudyPhotoService;
import com.s2000coaching.service.PhotoPairing.PeriodPhotos;
import com.s2000coaching.web.dto.CaseStudyPhotoResponse;
import com.s2000coaching.web.dto.PublishPhotosRequest;
import java.time.LocalDate;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * The coach's side of a case study: create it, upload data into it, look at the analysis,
 * then publish it.
 *
 * <p>Uploading is two steps on purpose. The preview returns the guessed column mapping and
 * the first rows as they were actually parsed, and nothing is written until the coach sends
 * that mapping back. A silent import is how a column of waist measurements ends up stored
 * as weights — which then looks like an ordinary, slightly surprising chart.
 *
 * <p>Every route here is behind the coach session gate in {@code WebConfig}.
 */
@RestController
public class AdminCaseStudyController {

	private final CaseStudyService caseStudyService;
	private final CaseStudyPhotoService photoService;
	private final CaseStudyImportService importService;
	private final ObjectMapper objectMapper;

	public AdminCaseStudyController(CaseStudyService caseStudyService, CaseStudyPhotoService photoService,
			CaseStudyImportService importService, ObjectMapper objectMapper) {
		this.caseStudyService = caseStudyService;
		this.photoService = photoService;
		this.importService = importService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/api/admin/case-studies")
	public List<CaseStudyResponse> list() {
		return caseStudyService.listForCoach();
	}

	@GetMapping("/api/admin/case-studies/{id}")
	public CaseStudyResponse read(@PathVariable UUID id) {
		return caseStudyService.readForCoach(id);
	}

	@PostMapping("/api/admin/case-studies")
	public CaseStudyResponse create(@Valid @RequestBody UpsertCaseStudyRequest request) {
		return caseStudyService.create(request);
	}

	@PutMapping("/api/admin/case-studies/{id}")
	public CaseStudyResponse update(@PathVariable UUID id,
			@Valid @RequestBody UpsertCaseStudyRequest request) {
		return caseStudyService.update(id, request);
	}

	@DeleteMapping("/api/admin/case-studies/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		caseStudyService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/admin/case-studies/{id}/publish")
	public CaseStudyResponse publish(@PathVariable UUID id) {
		return caseStudyService.setPublished(id, true);
	}

	@PostMapping("/api/admin/case-studies/{id}/unpublish")
	public CaseStudyResponse unpublish(@PathVariable UUID id) {
		return caseStudyService.setPublished(id, false);
	}

	/** The analysis exactly as the public page would show it, before deciding to publish. */
	@GetMapping("/api/admin/case-studies/{id}/analysis")
	public BodyAnalysis analysis(@PathVariable UUID id) {
		return caseStudyService.analyzeForCoach(id);
	}

	@PostMapping(value = "/api/admin/case-studies/{id}/preview",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CsvPreviewResponse preview(@PathVariable UUID id, @RequestParam("kind") DataKind kind,
			@RequestParam("file") MultipartFile file) throws IOException {
		// The study is required to exist even though nothing is written: previewing against an
		// id that was deleted in another tab should fail here, not after the coach has spent
		// time correcting a mapping.
		caseStudyService.require(id);
		byte[] content = file.getBytes();
		return kind == DataKind.MEASUREMENTS
				? importService.previewMeasurements(content)
				: importService.previewIntake(content);
	}

	@PostMapping(value = "/api/admin/case-studies/{id}/import",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImportResultResponse importData(@PathVariable UUID id, @RequestParam("kind") DataKind kind,
			@RequestParam("file") MultipartFile file, @RequestParam("mapping") String mappingJson)
			throws IOException {
		var study = caseStudyService.require(id);
		Map<String, Integer> mapping = parseMapping(mappingJson);
		byte[] content = file.getBytes();

		ImportResultResponse result = kind == DataKind.MEASUREMENTS
				? importService.importMeasurements(study, content, mapping)
				: importService.importIntake(study, content, mapping);
		caseStudyService.touch(id);
		return result;
	}

	/**
	 * Wipes one kind of data. Needed because re-importing a corrected file cannot undo a
	 * mis-mapped one: the bad rows sit on dates the new file may not mention, so the upsert
	 * leaves them exactly where they are.
	 */
	@DeleteMapping("/api/admin/case-studies/{id}/data")
	public ResponseEntity<Void> clearData(@PathVariable UUID id, @RequestParam("kind") DataKind kind) {
		caseStudyService.clearData(id, kind == DataKind.MEASUREMENTS);
		return ResponseEntity.noContent().build();
	}

	// --- photos -------------------------------------------------------------------------

	@GetMapping("/api/admin/case-studies/{id}/photos")
	public List<CaseStudyPhotoResponse> photos(@PathVariable UUID id) {
		return photoService.list(id);
	}

	@PostMapping(value = "/api/admin/case-studies/{id}/photos",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CaseStudyPhotoResponse uploadPhoto(@PathVariable UUID id,
			@RequestParam("file") MultipartFile file,
			@RequestParam("takenOn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenOn,
			@RequestParam("pose") PhotoPose pose) throws IOException {
		return photoService.upload(id, takenOn, pose, file.getBytes());
	}

	@DeleteMapping("/api/admin/case-study-photos/{photoId}")
	public ResponseEntity<Void> deletePhoto(@PathVariable UUID photoId) {
		photoService.delete(photoId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/admin/case-study-photos/{photoId}/content")
	public ResponseEntity<InputStreamResource> photoContent(@PathVariable UUID photoId) {
		return CaseStudyController.imageResponse(photoService.readAsCoach(photoId));
	}

	/** The pairing exactly as the public page would show it, before photos are switched on. */
	@GetMapping("/api/admin/case-studies/{id}/photo-periods")
	public List<PeriodPhotos> photoPeriods(@PathVariable UUID id) {
		return caseStudyService.photoPeriodsForCoach(id);
	}

	/** Separate from {@code /publish}: showing the numbers is not consent to show the body. */
	@PostMapping("/api/admin/case-studies/{id}/photos/publish")
	public CaseStudyResponse publishPhotos(@PathVariable UUID id,
			@Valid @RequestBody PublishPhotosRequest request) {
		return caseStudyService.setPhotosPublished(id, true);
	}

	@PostMapping("/api/admin/case-studies/{id}/photos/unpublish")
	public CaseStudyResponse unpublishPhotos(@PathVariable UUID id) {
		return caseStudyService.setPhotosPublished(id, false);
	}

	private Map<String, Integer> parseMapping(String mappingJson) {
		try {
			Map<String, Integer> mapping = objectMapper.readValue(mappingJson,
					new TypeReference<Map<String, Integer>>() {
					});
			if (mapping == null || mapping.isEmpty()) {
				throw new BadRequestException("Sütun eşlemesi boş.");
			}
			return mapping;
		} catch (JacksonException e) {
			throw new BadRequestException("Sütun eşlemesi okunamadı.");
		}
	}

	/** Which of the two tables an upload is for. */
	public enum DataKind {
		MEASUREMENTS,
		INTAKE
	}
}
