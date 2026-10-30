package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.WeeklyNutrition;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.WeeklyNutritionRepository;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.web.dto.WeeklyNutritionResponse;
import java.io.InputStream;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The weekly food album. Each week (Monday to Sunday) holds the week's total calories, how they
 * split into carbohydrate, fat and protein (percentages that must add up to 100), and at most one
 * photo. The member enters it; the coach reads it.
 *
 * <p>What is entered is health data, so nothing is written before the member has consented —
 * deleting stays allowed whatever the consent says. Saving a week that already exists replaces it,
 * which is how an entry is corrected.
 */
@Service
public class WeeklyNutritionService implements MemberDataEraser {

	private static final Logger log = LoggerFactory.getLogger(WeeklyNutritionService.class);

	/** Same rule as progress photos: what a browser shows as it is, and nothing that can carry a script. */
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	static final int MIN_CALORIES = 1;
	static final int MAX_CALORIES = 100_000;
	/** How far back a week may be filled in; a typo in the year should not create a 1970 entry. */
	static final int MAX_WEEKS_BACK = 156;

	/** A photo that came with the request: its declared type and its bytes. */
	public record PhotoUpload(String contentType, byte[] content) {
	}

	private final WeeklyNutritionRepository repository;
	private final PhotoStorage photoStorage;
	private final MemberService memberService;
	private final Clock clock;
	private final long maxPhotoBytes;

	public WeeklyNutritionService(WeeklyNutritionRepository repository, PhotoStorage photoStorage,
			MemberService memberService, Clock clock, @Value("${app.photos.max-size-bytes}") long maxPhotoBytes) {
		this.repository = repository;
		this.photoStorage = photoStorage;
		this.memberService = memberService;
		this.clock = clock;
		this.maxPhotoBytes = maxPhotoBytes;
	}

	@Transactional(readOnly = true)
	public List<WeeklyNutritionResponse> list(UUID memberId) {
		return repository.findAllByMemberIdOrderByWeekStartDesc(memberId).stream()
				.map(WeeklyNutritionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<WeeklyNutritionResponse> listAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return list(memberId);
	}

	/**
	 * Creates the week, or replaces it when it already exists. {@code photo} is a new photo (replacing
	 * any earlier one); with none, an earlier photo stays unless {@code removePhoto} says to drop it.
	 */
	@Transactional
	public WeeklyNutritionResponse save(Member member, LocalDate weekStart, int calories, int carbsPercent,
			int fatPercent, int proteinPercent, PhotoUpload photo, boolean removePhoto) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Haftalık beslenme girebilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}
		validateWeek(weekStart);
		validateFigures(calories, carbsPercent, fatPercent, proteinPercent);
		String contentType = photo == null ? null : validatePhoto(photo);

		Instant now = Instant.now(clock);
		Optional<WeeklyNutrition> earlier = repository.findByMemberIdAndWeekStart(member.getId(), weekStart);
		WeeklyNutrition entry = earlier.orElseGet(() -> new WeeklyNutrition(UUID.randomUUID(), member, weekStart,
				calories, carbsPercent, fatPercent, proteinPercent, now));
		entry.update(calories, carbsPercent, fatPercent, proteinPercent, now);

		String oldKey = entry.getPhotoKey();
		boolean replaced = false;
		if (photo != null) {
			// Bytes first, then the row: a row pointing at nothing is a broken picture, bytes with no
			// row are invisible and can be reclaimed.
			entry.attachPhoto(photoStorage.store(photo.content(), contentType), contentType, photo.content().length);
			replaced = true;
		} else if (removePhoto && entry.hasPhoto()) {
			entry.clearPhoto();
			replaced = true;
		}
		if (earlier.isEmpty()) {
			entry = repository.save(entry);
		}
		if (replaced && oldKey != null) {
			deleteFileAfterCommit(oldKey);
		}
		return WeeklyNutritionResponse.from(entry);
	}

	@Transactional(readOnly = true)
	public StoredPhoto readOwnPhoto(Member member, UUID id) {
		WeeklyNutrition entry = getOrThrow(id);
		if (!entry.getMember().getId().equals(member.getId())) {
			// Not 403: answering differently would confirm the id exists.
			throw new NotFoundException("Kayıt bulunamadı.");
		}
		return readPhoto(entry);
	}

	@Transactional(readOnly = true)
	public StoredPhoto readPhotoAsCoach(UUID id) {
		return readPhoto(getOrThrow(id));
	}

	@Transactional
	public void deleteOwn(Member member, UUID id) {
		WeeklyNutrition entry = getOrThrow(id);
		if (!entry.getMember().getId().equals(member.getId())) {
			throw new NotFoundException("Kayıt bulunamadı.");
		}
		delete(entry);
	}

	@Transactional
	public void deleteAsCoach(UUID id) {
		delete(getOrThrow(id));
	}

	/** Rows and photo files both: leaving the pictures of someone who asked to be erased would defeat it. */
	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		for (WeeklyNutrition entry : repository.findAllByMemberIdOrderByWeekStartDesc(memberId)) {
			deleteFile(entry.getPhotoKey());
		}
		repository.deleteAllByMemberId(memberId);
	}

	/** The Monday of the week {@code date} falls in. */
	static LocalDate mondayOf(LocalDate date) {
		return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private void validateWeek(LocalDate weekStart) {
		if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
			throw new BadRequestException("Hafta pazartesi ile başlamalı (pazartesi–pazar).");
		}
		LocalDate thisMonday = mondayOf(LocalDate.now(clock));
		if (weekStart.isAfter(thisMonday)) {
			throw new BadRequestException("Henüz başlamamış bir hafta için kayıt girilemez.");
		}
		if (weekStart.isBefore(thisMonday.minusWeeks(MAX_WEEKS_BACK))) {
			throw new BadRequestException("Bu hafta çok eski; en fazla üç yıl öncesine kayıt girilebilir.");
		}
	}

	private static void validateFigures(int calories, int carbs, int fat, int protein) {
		if (calories < MIN_CALORIES || calories > MAX_CALORIES) {
			throw new BadRequestException(
					"Haftalık toplam kalori %d ile %d arasında olmalı.".formatted(MIN_CALORIES, MAX_CALORIES));
		}
		if (carbs < 0 || fat < 0 || protein < 0 || carbs > 100 || fat > 100 || protein > 100) {
			throw new BadRequestException("Yüzdeler 0 ile 100 arasında olmalı.");
		}
		int total = carbs + fat + protein;
		if (total != 100) {
			throw new BadRequestException(
					"Karbonhidrat, yağ ve protein yüzdelerinin toplamı 100 olmalı (şu an %d).".formatted(total));
		}
	}

	/** Returns the normalised content type when the photo is acceptable. */
	private String validatePhoto(PhotoUpload photo) {
		String type = photo.contentType() == null ? "" : photo.contentType().toLowerCase(Locale.ROOT).trim();
		if (!ALLOWED_CONTENT_TYPES.contains(type)) {
			throw new BadRequestException("Yalnızca JPEG, PNG veya WebP fotoğraf yükleyebilirsiniz.");
		}
		if (photo.content().length == 0) {
			throw new BadRequestException("Fotoğraf dosyası boş.");
		}
		if (photo.content().length > maxPhotoBytes) {
			throw new BadRequestException(
					"Fotoğraf en fazla %d MB olabilir.".formatted(maxPhotoBytes / (1024 * 1024)));
		}
		return type;
	}

	private StoredPhoto readPhoto(WeeklyNutrition entry) {
		if (!entry.hasPhoto()) {
			throw new NotFoundException("Bu haftanın fotoğrafı yok.");
		}
		InputStream content = photoStorage.read(entry.getPhotoKey());
		return new StoredPhoto(content, entry.getPhotoContentType(), entry.getPhotoSizeBytes());
	}

	private void delete(WeeklyNutrition entry) {
		String key = entry.getPhotoKey();
		repository.delete(entry);
		if (key != null) {
			deleteFileAfterCommit(key);
		}
	}

	/** The old file goes only once the change is committed; a rollback must not leave a row without its picture. */
	private void deleteFileAfterCommit(String key) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					deleteFile(key);
				}
			});
		} else {
			deleteFile(key);
		}
	}

	/** A file that will not delete is only wasted bytes; it must not stop the row from going. */
	private void deleteFile(String key) {
		if (key == null) {
			return;
		}
		try {
			photoStorage.delete(key);
		} catch (RuntimeException e) {
			log.warn("Haftalık beslenme fotoğrafı silinemedi: {}", key, e);
		}
	}

	private WeeklyNutrition getOrThrow(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Kayıt bulunamadı."));
	}

	/** The bytes plus what the browser needs to render them. */
	public record StoredPhoto(InputStream content, String contentType, long sizeBytes) {
	}
}
