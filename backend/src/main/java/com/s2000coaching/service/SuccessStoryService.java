package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.ProgressPhoto;
import com.s2000coaching.domain.StoryStatus;
import com.s2000coaching.domain.SuccessStory;
import com.s2000coaching.domain.SuccessStoryPhoto;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ProgressPhotoRepository;
import com.s2000coaching.repository.SuccessStoryPhotoRepository;
import com.s2000coaching.repository.SuccessStoryRepository;
import com.s2000coaching.service.ProgressPhotoService.StoredPhoto;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.web.dto.CreateStoryRequest;
import com.s2000coaching.web.dto.PublicStoryResponse;
import com.s2000coaching.web.dto.StoryPhotoResponse;
import com.s2000coaching.web.dto.StoryPhotoSelection;
import com.s2000coaching.web.dto.StoryResponse;
import com.s2000coaching.web.dto.UpdateStoryRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Success stories with real photos — the one place member photos can end up on the public site,
 * and so the place with the most rules.
 *
 * <ul>
 * <li>The photos are <em>copied</em> (re-encoded, metadata stripped) into the story's own
 * storage keys. The public endpoints read only story tables, never a member's photo.</li>
 * <li>Only the member can move a story to consented, by agreeing to its exact content; the
 * wording they agreed to is stored. Editing the content afterwards voids that consent.</li>
 * <li>Publishing needs consent. The member can withdraw at any time, which deletes the copies.</li>
 * <li>Deleting a member deletes their stories and the copies.</li>
 * </ul>
 */
@Service
public class SuccessStoryService implements MemberDataEraser {

	private static final Logger log = LoggerFactory.getLogger(SuccessStoryService.class);

	static final int MAX_PHOTOS = 6;

	/** Shown to the member next to the story, and stored with their consent. */
	public static final String CONSENT_TEXT = """
			Yukarıda gördüğüm hikâyemin, adımın (görünen ad) ve seçilen fotoğraflarımın S2000 Coaching web \
			sitesinde herkese açık olarak yayınlanmasına, kendi özgür irademe dayanarak açık rıza veriyorum. \
			Fotoğraflar vücut görüntümü içerdiği için özel nitelikli kişisel veri sayılır; rızamın yalnızca bu \
			içerik için geçerli olduğunu, içerik değiştirilirse yeniden onayım istenmesi gerektiğini biliyorum. \
			Rızamı istediğim zaman, hiçbir gerekçe göstermeden geri çekebilirim; geri çektiğimde içerik yayından \
			kalkar ve fotoğraf kopyaları silinir. Yayın süresince üçüncü kişilerin sayfayı kaydetmiş olabileceğini \
			ve bu kopyalar üzerinde kontrol sahibi olunamayacağını anlıyorum.""";

	private final SuccessStoryRepository storyRepository;
	private final SuccessStoryPhotoRepository photoRepository;
	private final ProgressPhotoRepository progressPhotoRepository;
	private final PhotoStorage photoStorage;
	private final MemberService memberService;
	private final Clock clock;

	public SuccessStoryService(SuccessStoryRepository storyRepository, SuccessStoryPhotoRepository photoRepository,
			ProgressPhotoRepository progressPhotoRepository, PhotoStorage photoStorage, MemberService memberService,
			Clock clock) {
		this.storyRepository = storyRepository;
		this.photoRepository = photoRepository;
		this.progressPhotoRepository = progressPhotoRepository;
		this.photoStorage = photoStorage;
		this.memberService = memberService;
		this.clock = clock;
	}

	// --- coach ---------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<StoryResponse> listForCoach() {
		return storyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::coachView).toList();
	}

	@Transactional
	public StoryResponse create(CreateStoryRequest request) {
		Member member = memberService.requireMember(request.memberId());
		Instant now = Instant.now(clock);
		SuccessStory story = storyRepository.save(new SuccessStory(UUID.randomUUID(), member,
				request.title().trim(), blankToNull(request.quote()), request.body().trim(),
				request.displayName().trim(), now));
		for (StoryPhotoSelection selection : request.photos()) {
			copyPhoto(story, selection);
		}
		return coachView(story);
	}

	@Transactional
	public StoryResponse update(UUID id, UpdateStoryRequest request) {
		SuccessStory story = editable(id);
		story.changeContent(request.title().trim(), blankToNull(request.quote()), request.body().trim(),
				request.displayName().trim(), Instant.now(clock));
		return coachView(story);
	}

	@Transactional
	public StoryResponse addPhoto(UUID id, StoryPhotoSelection selection) {
		SuccessStory story = editable(id);
		copyPhoto(story, selection);
		story.voidConsent(Instant.now(clock));
		return coachView(story);
	}

	@Transactional
	public StoryResponse removePhoto(UUID photoId) {
		SuccessStoryPhoto photo = photoRepository.findById(photoId)
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı."));
		SuccessStory story = editable(photo.getStoryId());
		deleteFile(photo);
		photoRepository.delete(photo);
		story.voidConsent(Instant.now(clock));
		return coachView(story);
	}

	@Transactional
	public StoryResponse sendForConsent(UUID id) {
		SuccessStory story = require(id);
		if (story.getStatus() != StoryStatus.DRAFT) {
			throw new ConflictException("Yalnızca taslak hikâye üyeye onaya gönderilebilir.");
		}
		if (photoRepository.countByStoryId(id) == 0) {
			throw new BadRequestException("Onaya göndermek için en az bir fotoğraf ekleyin.");
		}
		story.sendForConsent(Instant.now(clock));
		return coachView(story);
	}

	/** Only a story the member has consented to can go public; there is no way around that here. */
	@Transactional
	public StoryResponse publish(UUID id) {
		SuccessStory story = require(id);
		if (story.getStatus() != StoryStatus.CONSENTED) {
			throw new ConflictException("Yayınlamak için üyenin onayı gerekiyor.");
		}
		story.publish(Instant.now(clock));
		return coachView(story);
	}

	@Transactional
	public StoryResponse unpublish(UUID id) {
		SuccessStory story = require(id);
		if (story.getStatus() != StoryStatus.PUBLISHED) {
			throw new ConflictException("Bu hikâye yayında değil.");
		}
		story.unpublish(Instant.now(clock));
		return coachView(story);
	}

	@Transactional
	public void delete(UUID id) {
		SuccessStory story = require(id);
		deleteAllPhotos(id);
		storyRepository.delete(story);
	}

	@Transactional(readOnly = true)
	public StoredPhoto readPhotoAsCoach(UUID photoId) {
		return read(photoRepository.findById(photoId).orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı.")));
	}

	// --- member --------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<StoryResponse> listOwn(Member member) {
		return storyRepository.findAllByMemberIdAndStatusNotOrderByCreatedAtDesc(member.getId(), StoryStatus.DRAFT)
				.stream().map(story -> StoryResponse.forMember(story, photosOf(story.getId()), CONSENT_TEXT)).toList();
	}

	@Transactional
	public StoryResponse consent(Member member, UUID id) {
		SuccessStory story = ownedBy(member, id);
		if (story.getStatus() != StoryStatus.AWAITING_CONSENT) {
			throw new ConflictException("Bu hikâye şu an onayınızı beklemiyor.");
		}
		story.consent(CONSENT_TEXT, Instant.now(clock));
		return StoryResponse.forMember(story, photosOf(id), CONSENT_TEXT);
	}

	@Transactional
	public StoryResponse decline(Member member, UUID id) {
		SuccessStory story = ownedBy(member, id);
		if (story.getStatus() != StoryStatus.AWAITING_CONSENT) {
			throw new ConflictException("Bu hikâye şu an onayınızı beklemiyor.");
		}
		story.decline(Instant.now(clock));
		// A "no" means the copies have no reason to exist.
		deleteAllPhotos(id);
		return StoryResponse.forMember(story, List.of(), CONSENT_TEXT);
	}

	/** Withdrawing takes the story off the site at once and deletes the photo copies. */
	@Transactional
	public StoryResponse revoke(Member member, UUID id) {
		SuccessStory story = ownedBy(member, id);
		if (story.getStatus() != StoryStatus.CONSENTED && story.getStatus() != StoryStatus.PUBLISHED
				&& story.getStatus() != StoryStatus.AWAITING_CONSENT) {
			throw new ConflictException("Geri çekilecek bir onayınız yok.");
		}
		story.revoke(Instant.now(clock));
		deleteAllPhotos(id);
		return StoryResponse.forMember(story, List.of(), CONSENT_TEXT);
	}

	@Transactional(readOnly = true)
	public StoredPhoto readPhotoAsMember(Member member, UUID photoId) {
		SuccessStoryPhoto photo = photoRepository.findById(photoId)
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı."));
		ownedBy(member, photo.getStoryId());
		return read(photo);
	}

	// --- public --------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<PublicStoryResponse> listPublished() {
		return storyRepository.findAllByStatusOrderByPublishedAtDesc(StoryStatus.PUBLISHED).stream()
				.map(story -> PublicStoryResponse.from(story, photosOf(story.getId())))
				.toList();
	}

	/** Story id and PUBLISHED in one query: a photo of anything else is simply not found. */
	@Transactional(readOnly = true)
	public StoredPhoto readPublicPhoto(UUID storyId, UUID photoId) {
		SuccessStory story = storyRepository.findByIdAndStatus(storyId, StoryStatus.PUBLISHED)
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı."));
		return read(photoRepository.findByIdAndStoryId(photoId, story.getId())
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı.")));
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		for (SuccessStory story : storyRepository.findAllByMemberId(memberId)) {
			deleteAllPhotos(story.getId());
			storyRepository.delete(story);
		}
	}

	// --- internals -----------------------------------------------------------------------

	private SuccessStory require(UUID id) {
		return storyRepository.findById(id).orElseThrow(() -> new NotFoundException("Hikâye bulunamadı."));
	}

	/** A story the coach may still change: not one the member declined or withdrew from. */
	private SuccessStory editable(UUID id) {
		SuccessStory story = require(id);
		if (story.getStatus() == StoryStatus.DECLINED || story.getStatus() == StoryStatus.REVOKED) {
			throw new ConflictException("Üye bu hikâyeyi reddetti ya da onayını geri çekti; yeni bir hikâye oluşturun.");
		}
		return story;
	}

	/** Same answer as a missing story, so one member cannot probe for another's ids. */
	private SuccessStory ownedBy(Member member, UUID storyId) {
		SuccessStory story = storyRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("Hikâye bulunamadı."));
		if (!story.getMember().getId().equals(member.getId())) {
			throw new NotFoundException("Hikâye bulunamadı.");
		}
		return story;
	}

	/**
	 * Copies a member's photo into the story: read, decode and re-encode (which also strips
	 * location and device metadata), store under a new key. Photos that cannot be cleaned are
	 * refused rather than published as they are.
	 */
	private void copyPhoto(SuccessStory story, StoryPhotoSelection selection) {
		ProgressPhoto source = progressPhotoRepository.findById(selection.progressPhotoId())
				.orElseThrow(() -> new NotFoundException("Fotoğraf bulunamadı."));
		if (!source.getMember().getId().equals(story.getMember().getId())) {
			throw new BadRequestException("Seçilen fotoğraf bu üyeye ait değil.");
		}
		long existing = photoRepository.countByStoryId(story.getId());
		if (existing >= MAX_PHOTOS) {
			throw new BadRequestException("Bir hikâyeye en fazla %d fotoğraf eklenebilir.".formatted(MAX_PHOTOS));
		}
		byte[] original;
		try (InputStream in = photoStorage.read(source.getStorageKey())) {
			original = in.readAllBytes();
		} catch (IOException e) {
			throw new BadRequestException("Fotoğraf okunamadı.");
		}
		PhotoSanitizer.Sanitized clean = PhotoSanitizer.sanitize(original);
		String key = photoStorage.store(clean.content(), clean.contentType());
		photoRepository.save(new SuccessStoryPhoto(UUID.randomUUID(), story.getId(), source.getTakenOn(),
				selection.caption().trim(), (int) existing, key, clean.contentType(), clean.content().length,
				Instant.now(clock)));
	}

	private StoryResponse coachView(SuccessStory story) {
		return StoryResponse.forCoach(story, photosOf(story.getId()));
	}

	private List<StoryPhotoResponse> photosOf(UUID storyId) {
		return photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(storyId).stream()
				.map(StoryPhotoResponse::from)
				.toList();
	}

	private StoredPhoto read(SuccessStoryPhoto photo) {
		return new StoredPhoto(photoStorage.read(photo.getStorageKey()), photo.getContentType(), photo.getSizeBytes());
	}

	private void deleteAllPhotos(UUID storyId) {
		for (SuccessStoryPhoto photo : photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(storyId)) {
			deleteFile(photo);
		}
		photoRepository.deleteAllByStoryId(storyId);
	}

	/** A file that will not delete is logged, not fatal: the member's withdrawal must go through. */
	private void deleteFile(SuccessStoryPhoto photo) {
		try {
			photoStorage.delete(photo.getStorageKey());
		} catch (RuntimeException e) {
			log.warn("Hikâye fotoğrafı dosyası silinemedi: {}", photo.getStorageKey(), e);
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
