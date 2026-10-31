package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.domain.ProgressPhoto;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ProgressPhotoRepository;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.web.dto.ProgressPhotoResponse;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progress photos. The member uploads and deletes their own; the coach reads them and can
 * delete one, the same shape as measurements.
 */
@Service
public class ProgressPhotoService {

	private static final Logger log = LoggerFactory.getLogger(ProgressPhotoService.class);

	/** What a browser can display without conversion, and nothing that can carry a script. */
	private static final Set<String> ALLOWED_CONTENT_TYPES =
			Set.of("image/jpeg", "image/png", "image/webp");

	private final ProgressPhotoRepository photoRepository;
	private final PhotoStorage photoStorage;
	private final MemberService memberService;
	private final long maxBytes;
	private final int maxPerMember;

	public ProgressPhotoService(ProgressPhotoRepository photoRepository,
			PhotoStorage photoStorage,
			MemberService memberService,
			@Value("${app.photos.max-size-bytes}") long maxBytes,
			@Value("${app.photos.max-per-member}") int maxPerMember) {
		this.photoRepository = photoRepository;
		this.photoStorage = photoStorage;
		this.memberService = memberService;
		this.maxBytes = maxBytes;
		this.maxPerMember = maxPerMember;
	}

	@Transactional(readOnly = true)
	public List<ProgressPhotoResponse> listForMember(UUID memberId) {
		return photoRepository.findAllByMemberIdOrderByTakenOnDesc(memberId).stream()
				.map(ProgressPhotoResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ProgressPhotoResponse> listForMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return listForMember(memberId);
	}

	/**
	 * Stores the bytes, then the row. Done in that order on purpose: a row pointing at
	 * nothing is a broken photo in the UI, while bytes with no row are invisible and
	 * reclaimable, so a failure between the two should land on the harmless side.
	 */
	@Transactional
	public ProgressPhotoResponse upload(Member member, LocalDate takenOn, PhotoPose pose, String contentType,
			byte[] content, String note) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Fotoğraf yükleyebilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}
		if (takenOn.isAfter(LocalDate.now())) {
			throw new BadRequestException("Çekim tarihi gelecekte olamaz.");
		}

		String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
		if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
			throw new BadRequestException("Yalnızca JPEG, PNG veya WebP fotoğraf yükleyebilirsiniz.");
		}
		if (content.length == 0) {
			throw new BadRequestException("Fotoğraf dosyası boş.");
		}
		if (content.length > maxBytes) {
			throw new BadRequestException(
					"Fotoğraf en fazla %d MB olabilir.".formatted(maxBytes / (1024 * 1024)));
		}
		java.time.YearMonth month = java.time.YearMonth.from(takenOn);
		if (photoRepository.existsByMemberIdAndPoseAndTakenOnBetween(member.getId(), pose,
				month.atDay(1), month.atEndOfMonth())) {
			throw new BadRequestException(
					"Bu ay için bu pozun fotoğrafı zaten yüklendi. Değiştirmek için önce mevcut fotoğrafı silin.");
		}
		if (photoRepository.countByMemberId(member.getId()) >= maxPerMember) {
			throw new BadRequestException(
					"En fazla %d fotoğraf saklanabilir. Yeni yüklemek için eskilerden silin."
							.formatted(maxPerMember));
		}

		String storageKey = photoStorage.store(content, normalizedType);
		ProgressPhoto photo = photoRepository.save(new ProgressPhoto(UUID.randomUUID(), member, takenOn,
				pose, storageKey, normalizedType, content.length, trimmedOrNull(note), Instant.now()));
		return ProgressPhotoResponse.from(photo);
	}

	/**
	 * The bytes, for the member who owns the photo. Callers hand in the member resolved from
	 * the token, so one member cannot read another's photo by guessing an id.
	 */
	@Transactional(readOnly = true)
	public StoredPhoto readOwn(Member member, UUID photoId) {
		ProgressPhoto photo = getOrThrow(photoId);
		if (!photo.getMember().getId().equals(member.getId())) {
			// Not 403: answering differently here would confirm the id exists.
			throw new NotFoundException("Fotoğraf bulunamadı.");
		}
		return read(photo);
	}

	@Transactional(readOnly = true)
	public StoredPhoto readAsCoach(UUID photoId) {
		return read(getOrThrow(photoId));
	}

	/*
	 * There is deliberately no member-side delete: the monthly set is final once saved. The coach can remove a
	 * photo (deleteAsCoach), and deleting the whole account removes them all (deleteAllOfMember).
	 */

	@Transactional
	public void deleteAsCoach(UUID photoId) {
		delete(getOrThrow(photoId));
	}

	/**
	 * Removes every photo of a member, files included. Called when the member is deleted:
	 * dropping only the rows would leave the images on disk forever, which is exactly what a
	 * deletion request under KVKK is asking not to happen.
	 */
	@Transactional
	public void deleteAllOfMember(UUID memberId) {
		for (ProgressPhoto photo : photoRepository.findAllByMemberIdOrderByTakenOnDesc(memberId)) {
			deleteStoredFile(photo);
		}
		photoRepository.deleteAllByMemberId(memberId);
	}

	private StoredPhoto read(ProgressPhoto photo) {
		InputStream content = photoStorage.read(photo.getStorageKey());
		return new StoredPhoto(content, photo.getContentType(), photo.getSizeBytes());
	}

	private void delete(ProgressPhoto photo) {
		deleteStoredFile(photo);
		photoRepository.delete(photo);
	}

	/**
	 * The row goes even when the file will not: a photo the member cannot delete because of a
	 * storage fault is worse than a key left behind, which is only wasted bytes.
	 */
	private void deleteStoredFile(ProgressPhoto photo) {
		try {
			photoStorage.delete(photo.getStorageKey());
		} catch (RuntimeException e) {
			log.warn("Fotoğraf dosyası silinemedi, kayıt yine de siliniyor: {}", photo.getStorageKey(), e);
		}
	}

	private ProgressPhoto getOrThrow(UUID photoId) {
		return photoRepository.findById(photoId)
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı."));
	}

	private static String trimmedOrNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	/** The bytes plus what the browser needs to render them. */
	public record StoredPhoto(InputStream content, String contentType, long sizeBytes) {
	}
}
