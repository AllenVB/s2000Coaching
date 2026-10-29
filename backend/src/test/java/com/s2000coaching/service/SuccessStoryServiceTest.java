package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.PhotoPose;
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
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.web.dto.CreateStoryRequest;
import com.s2000coaching.web.dto.StoryPhotoSelection;
import com.s2000coaching.web.dto.UpdateStoryRequest;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuccessStoryServiceTest {

	@Mock
	private SuccessStoryRepository storyRepository;
	@Mock
	private SuccessStoryPhotoRepository photoRepository;
	@Mock
	private ProgressPhotoRepository progressPhotoRepository;
	@Mock
	private PhotoStorage photoStorage;
	@Mock
	private MemberService memberService;

	private SuccessStoryService service;
	private Member member;
	private Member other;

	@BeforeEach
	void setUp() {
		service = new SuccessStoryService(storyRepository, photoRepository, progressPhotoRepository, photoStorage,
				memberService, Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC));
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet Yılmaz", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
		other = new Member(UUID.randomUUID(), "b@example.com", "Başka Kişi", "+905551112244", 170,
				new BigDecimal("70"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	private SuccessStory story(StoryStatus status) {
		SuccessStory story = new SuccessStory(UUID.randomUUID(), member, "Başlık", "Söz", "Hikâye metni", "Ahmet Y.",
				Instant.now());
		switch (status) {
			case DRAFT -> {
			}
			case AWAITING_CONSENT -> story.sendForConsent(Instant.now());
			case CONSENTED -> story.consent("metin", Instant.now());
			case PUBLISHED -> {
				story.consent("metin", Instant.now());
				story.publish(Instant.now());
			}
			case DECLINED -> story.decline(Instant.now());
			case REVOKED -> story.revoke(Instant.now());
		}
		return story;
	}

	private void exists(SuccessStory story) {
		when(storyRepository.findById(story.getId())).thenReturn(Optional.of(story));
	}

	private static byte[] jpegWithSecretMetadata() throws Exception {
		BufferedImage img = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
		var g = img.createGraphics();
		g.setColor(Color.RED);
		g.fillRect(0, 0, 20, 10);
		g.dispose();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "jpeg", out);
		byte[] jpeg = out.toByteArray();
		byte[] secret = "Exif\0\0GPSLatitude41.0082N".getBytes(StandardCharsets.ISO_8859_1);
		int length = secret.length + 2;
		ByteArrayOutputStream withExif = new ByteArrayOutputStream();
		withExif.write(jpeg, 0, 2);
		withExif.write(0xFF);
		withExif.write(0xE1);
		withExif.write(length >> 8);
		withExif.write(length & 0xFF);
		withExif.writeBytes(secret);
		withExif.write(jpeg, 2, jpeg.length - 2);
		return withExif.toByteArray();
	}

	// --- the consent gate: the property this whole feature exists to guarantee -------------

	@Nested
	class Publishing {

		@Test
		void shouldPublishOnlyAStoryTheMemberConsentedTo() {
			var consented = story(StoryStatus.CONSENTED);
			exists(consented);

			var response = service.publish(consented.getId());

			assertThat(response.status()).isEqualTo(StoryStatus.PUBLISHED);
			assertThat(response.publishedAt()).isEqualTo(Instant.parse("2026-10-27T09:00:00Z"));
		}

		@Test
		void shouldRefuseToPublishAnythingTheMemberHasNotConsentedTo() {
			for (StoryStatus status : List.of(StoryStatus.DRAFT, StoryStatus.AWAITING_CONSENT, StoryStatus.DECLINED,
					StoryStatus.REVOKED, StoryStatus.PUBLISHED)) {
				var story = story(status);
				exists(story);

				assertThatThrownBy(() -> service.publish(story.getId()))
						.as("publishing a %s story", status)
						.isInstanceOf(ConflictException.class);
				assertThat(story.getStatus()).isEqualTo(status);
			}
		}
	}

	@Nested
	class Consent {

		@Test
		void shouldStoreTheExactWordingTheMemberAgreedTo() {
			var story = story(StoryStatus.AWAITING_CONSENT);
			exists(story);

			var response = service.consent(member, story.getId());

			assertThat(response.status()).isEqualTo(StoryStatus.CONSENTED);
			assertThat(story.getConsentText()).isEqualTo(SuccessStoryService.CONSENT_TEXT);
			assertThat(story.getConsentAt()).isEqualTo(Instant.parse("2026-10-27T09:00:00Z"));
		}

		@Test
		void shouldNotLetSomeoneElseConsentOnTheMembersBehalf() {
			var story = story(StoryStatus.AWAITING_CONSENT);
			exists(story);

			assertThatThrownBy(() -> service.consent(other, story.getId())).isInstanceOf(NotFoundException.class);
			assertThat(story.getStatus()).isEqualTo(StoryStatus.AWAITING_CONSENT);
		}

		/** Only a story the member was actually asked about can be consented to. */
		@Test
		void shouldNotAcceptConsentForADraft() {
			var draft = story(StoryStatus.DRAFT);
			exists(draft);

			assertThatThrownBy(() -> service.consent(member, draft.getId())).isInstanceOf(ConflictException.class);
			assertThat(draft.getStatus()).isEqualTo(StoryStatus.DRAFT);
		}

		@Test
		void shouldDeleteTheCopiesWhenTheMemberDeclines() {
			var story = story(StoryStatus.AWAITING_CONSENT);
			exists(story);
			var copy = new SuccessStoryPhoto(UUID.randomUUID(), story.getId(), LocalDate.of(2026, 1, 1), "Önce", 0,
					"story-key", "image/jpeg", 10, Instant.now());
			when(photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(story.getId())).thenReturn(List.of(copy));

			var response = service.decline(member, story.getId());

			assertThat(response.status()).isEqualTo(StoryStatus.DECLINED);
			verify(photoStorage).delete("story-key");
			verify(photoRepository).deleteAllByStoryId(story.getId());
		}
	}

	@Nested
	class Withdrawing {

		@Test
		void shouldTakeAPublishedStoryDownAndDeleteTheCopies() {
			var story = story(StoryStatus.PUBLISHED);
			exists(story);
			var copy = new SuccessStoryPhoto(UUID.randomUUID(), story.getId(), LocalDate.of(2026, 1, 1), "Önce", 0,
					"story-key", "image/jpeg", 10, Instant.now());
			when(photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(story.getId())).thenReturn(List.of(copy));

			var response = service.revoke(member, story.getId());

			assertThat(response.status()).isEqualTo(StoryStatus.REVOKED);
			assertThat(response.publishedAt()).isNull();
			verify(photoStorage).delete("story-key");
			verify(photoRepository).deleteAllByStoryId(story.getId());
		}

		@Test
		void shouldStillWithdrawWhenAFileCannotBeDeleted() {
			var story = story(StoryStatus.PUBLISHED);
			exists(story);
			var copy = new SuccessStoryPhoto(UUID.randomUUID(), story.getId(), LocalDate.of(2026, 1, 1), "Önce", 0,
					"story-key", "image/jpeg", 10, Instant.now());
			when(photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(story.getId())).thenReturn(List.of(copy));
			org.mockito.Mockito.doThrow(new RuntimeException("disk")).when(photoStorage).delete("story-key");

			assertThat(service.revoke(member, story.getId()).status()).isEqualTo(StoryStatus.REVOKED);
		}

		@Test
		void shouldNotLetAnotherMemberWithdraw() {
			var story = story(StoryStatus.PUBLISHED);
			exists(story);

			assertThatThrownBy(() -> service.revoke(other, story.getId())).isInstanceOf(NotFoundException.class);
		}

		@Test
		void shouldNotAllowEditingAStoryTheMemberRejected() {
			var revoked = story(StoryStatus.REVOKED);
			exists(revoked);

			assertThatThrownBy(() -> service.update(revoked.getId(),
					new UpdateStoryRequest("Yeni", null, "Metin", "Ahmet Y."))).isInstanceOf(ConflictException.class);
		}
	}

	@Nested
	class Editing {

		/** The member agreed to what they saw, not to whatever it became. */
		@Test
		void shouldVoidConsentWhenTheWordsChange() {
			var published = story(StoryStatus.PUBLISHED);
			exists(published);

			var response = service.update(published.getId(),
					new UpdateStoryRequest("Başka başlık", null, "Değişmiş metin", "Ahmet Y."));

			assertThat(response.status()).isEqualTo(StoryStatus.DRAFT);
			assertThat(response.consentAt()).isNull();
			assertThat(response.publishedAt()).isNull();
			assertThat(published.getConsentText()).isNull();
		}

		@Test
		void shouldVoidConsentWhenAPhotoIsRemoved() {
			var consented = story(StoryStatus.CONSENTED);
			var copy = new SuccessStoryPhoto(UUID.randomUUID(), consented.getId(), LocalDate.of(2026, 1, 1), "Önce",
					0, "k", "image/jpeg", 10, Instant.now());
			when(photoRepository.findById(copy.getId())).thenReturn(Optional.of(copy));
			exists(consented);

			service.removePhoto(copy.getId());

			assertThat(consented.getStatus()).isEqualTo(StoryStatus.DRAFT);
			verify(photoStorage).delete("k");
		}

		@Test
		void shouldRequireAPhotoBeforeAskingTheMember() {
			var draft = story(StoryStatus.DRAFT);
			exists(draft);
			when(photoRepository.countByStoryId(draft.getId())).thenReturn(0L);

			assertThatThrownBy(() -> service.sendForConsent(draft.getId())).isInstanceOf(BadRequestException.class);
			assertThat(draft.getStatus()).isEqualTo(StoryStatus.DRAFT);
		}

		@Test
		void shouldSendADraftWithPhotosForConsent() {
			var draft = story(StoryStatus.DRAFT);
			exists(draft);
			when(photoRepository.countByStoryId(draft.getId())).thenReturn(2L);

			assertThat(service.sendForConsent(draft.getId()).status()).isEqualTo(StoryStatus.AWAITING_CONSENT);
		}
	}

	@Nested
	class CopyingPhotos {

		private ProgressPhoto progressPhoto(Member owner) {
			return new ProgressPhoto(UUID.randomUUID(), owner, LocalDate.of(2026, 1, 1), PhotoPose.ON, "member-key",
					"image/jpeg", 10, null, Instant.now());
		}

		@Test
		void shouldStoreACleanedCopyUnderANewKeyNeverTheOriginal() throws Exception {
			when(memberService.requireMember(member.getId())).thenReturn(member);
			var source = progressPhoto(member);
			byte[] original = jpegWithSecretMetadata();
			assertThat(new String(original, StandardCharsets.ISO_8859_1)).contains("GPSLatitude");
			when(progressPhotoRepository.findById(source.getId())).thenReturn(Optional.of(source));
			when(photoStorage.read("member-key")).thenReturn(new ByteArrayInputStream(original));
			when(photoStorage.store(any(), anyString())).thenReturn("story-key");
			when(storyRepository.save(any())).thenAnswer(call -> call.getArgument(0));

			service.create(new CreateStoryRequest(member.getId(), "Başlık", null, "Metin", "Ahmet Y.",
					List.of(new StoryPhotoSelection(source.getId(), "Önce"))));

			ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
			verify(photoStorage).store(stored.capture(), anyString());
			assertThat(new String(stored.getValue(), StandardCharsets.ISO_8859_1)).doesNotContain("GPSLatitude");
			ArgumentCaptor<SuccessStoryPhoto> row = ArgumentCaptor.forClass(SuccessStoryPhoto.class);
			verify(photoRepository).save(row.capture());
			assertThat(row.getValue().getStorageKey()).isEqualTo("story-key").isNotEqualTo(source.getStorageKey());
		}

		/** A story must never be able to pull in someone else's body photo. */
		@Test
		void shouldRefuseAPhotoOfAnotherMember() {
			when(memberService.requireMember(member.getId())).thenReturn(member);
			var source = progressPhoto(other);
			when(progressPhotoRepository.findById(source.getId())).thenReturn(Optional.of(source));
			when(storyRepository.save(any())).thenAnswer(call -> call.getArgument(0));

			assertThatThrownBy(() -> service.create(new CreateStoryRequest(member.getId(), "Başlık", null, "Metin",
					"Ahmet Y.", List.of(new StoryPhotoSelection(source.getId(), "Önce")))))
					.isInstanceOf(BadRequestException.class);
			verify(photoStorage, never()).store(any(), anyString());
		}

		@Test
		void shouldRefuseAPhotoThatCannotBeCleaned() {
			when(memberService.requireMember(member.getId())).thenReturn(member);
			var source = progressPhoto(member);
			when(progressPhotoRepository.findById(source.getId())).thenReturn(Optional.of(source));
			when(photoStorage.read("member-key"))
					.thenReturn(new ByteArrayInputStream("RIFF....WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)));
			when(storyRepository.save(any())).thenAnswer(call -> call.getArgument(0));

			assertThatThrownBy(() -> service.create(new CreateStoryRequest(member.getId(), "Başlık", null, "Metin",
					"Ahmet Y.", List.of(new StoryPhotoSelection(source.getId(), "Önce")))))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("WebP");
			verify(photoStorage, never()).store(any(), anyString());
		}
	}

	@Nested
	class PublicReads {

		@Test
		void shouldNotServeAPhotoOfAStoryThatIsNotPublished() {
			UUID storyId = UUID.randomUUID();
			when(storyRepository.findByIdAndStatus(storyId, StoryStatus.PUBLISHED)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.readPublicPhoto(storyId, UUID.randomUUID()))
					.isInstanceOf(NotFoundException.class);
			verify(photoStorage, never()).read(anyString());
		}

		@Test
		void shouldNotServeAPhotoThatBelongsToAnotherStory() {
			var published = story(StoryStatus.PUBLISHED);
			UUID photoId = UUID.randomUUID();
			when(storyRepository.findByIdAndStatus(published.getId(), StoryStatus.PUBLISHED))
					.thenReturn(Optional.of(published));
			when(photoRepository.findByIdAndStoryId(photoId, published.getId())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.readPublicPhoto(published.getId(), photoId))
					.isInstanceOf(NotFoundException.class);
		}

		@Test
		void shouldListOnlyPublishedStoriesWithoutAnythingIdentifying() {
			var published = story(StoryStatus.PUBLISHED);
			when(storyRepository.findAllByStatusOrderByPublishedAtDesc(StoryStatus.PUBLISHED))
					.thenReturn(List.of(published));
			when(photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(published.getId())).thenReturn(List.of());

			var stories = service.listPublished();

			assertThat(stories).hasSize(1);
			assertThat(stories.get(0).displayName()).isEqualTo("Ahmet Y.");
			assertThat(com.s2000coaching.web.dto.PublicStoryResponse.class.getRecordComponents())
					.extracting(java.lang.reflect.RecordComponent::getName)
					.doesNotContain("memberId", "memberName", "status", "consentAt", "consentText", "email");
		}
	}

	@Test
	void shouldDeleteStoriesAndTheirCopiesWhenTheMemberIsErased() {
		var story = story(StoryStatus.PUBLISHED);
		var copy = new SuccessStoryPhoto(UUID.randomUUID(), story.getId(), LocalDate.of(2026, 1, 1), "Önce", 0, "k",
				"image/jpeg", 10, Instant.now());
		when(storyRepository.findAllByMemberId(member.getId())).thenReturn(List.of(story));
		when(photoRepository.findAllByStoryIdOrderByDisplayOrderAsc(story.getId())).thenReturn(List.of(copy));

		service.eraseAllOf(member.getId());

		verify(photoStorage, times(1)).delete("k");
		verify(storyRepository).delete(story);
	}
}
