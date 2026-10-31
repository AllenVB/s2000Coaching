package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.domain.ProgressPhoto;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.ProgressPhotoRepository;
import com.s2000coaching.storage.PhotoStorage;
import com.s2000coaching.storage.PhotoStorageException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressPhotoServiceTest {

	private static final long MAX_BYTES = 1024 * 1024;
	private static final int MAX_PER_MEMBER = 3;
	private static final byte[] JPEG = new byte[] { 1, 2, 3 };

	@Mock
	private ProgressPhotoRepository photoRepository;

	@Mock
	private PhotoStorage photoStorage;

	@Mock
	private MemberService memberService;

	private ProgressPhotoService photoService;
	private Member member;

	private static Member member(String email) {
		return new Member(UUID.randomUUID(), email, "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
	}

	@BeforeEach
	void setUp() {
		photoService = new ProgressPhotoService(photoRepository, photoStorage, memberService, MAX_BYTES,
				MAX_PER_MEMBER);
		member = member("sporcu@example.com");
		member.grantHealthDataConsent(Instant.now());
	}

	/** Keys are unique in the database, so each fixture gets its own. */
	private ProgressPhoto photoOf(Member owner) {
		return photoOf(owner, "ab/cd/" + UUID.randomUUID() + ".jpg");
	}

	private ProgressPhoto photoOf(Member owner, String storageKey) {
		return new ProgressPhoto(UUID.randomUUID(), owner, LocalDate.now(), PhotoPose.ON, storageKey,
				"image/jpeg", 3, null, Instant.now());
	}

	@Nested
	class Uploading {

		@Test
		void shouldRefuseASecondPhotoOfTheSamePoseInTheSameMonth() {
			LocalDate day = LocalDate.now();
			when(photoRepository.existsByMemberIdAndPoseAndTakenOnBetween(member.getId(), PhotoPose.ON_BICEPS,
					day.withDayOfMonth(1), java.time.YearMonth.from(day).atEndOfMonth())).thenReturn(true);

			assertThatThrownBy(() -> photoService.upload(member, day, PhotoPose.ON_BICEPS, "image/jpeg", JPEG, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Bu ay");
			verify(photoStorage, never()).store(any(), any());
		}

		@Test
		void shouldStoreTheBytesAndKeepOnlyTheKey() {
			when(photoStorage.store(JPEG, "image/jpeg")).thenReturn("ab/cd/key.jpg");
			when(photoRepository.save(any())).thenAnswer(call -> call.getArgument(0));

			var response = photoService.upload(member, LocalDate.now(), PhotoPose.ON, "image/jpeg", JPEG,
					"  ilk fotoğraf  ");

			assertThat(response.pose()).isEqualTo(PhotoPose.ON);
			assertThat(response.sizeBytes()).isEqualTo(3);
			assertThat(response.note()).isEqualTo("ilk fotoğraf");

			var saved = ArgumentCaptor.forClass(ProgressPhoto.class);
			verify(photoRepository).save(saved.capture());
			assertThat(saved.getValue().getStorageKey()).isEqualTo("ab/cd/key.jpg");
		}

		@Test
		void shouldRefuseWithoutHealthDataConsent() {
			Member withoutConsent = member("baska@example.com");

			assertThatThrownBy(() -> photoService.upload(withoutConsent, LocalDate.now(), PhotoPose.ON,
					"image/jpeg", JPEG, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("sağlık verisi işleme onayını");
			verify(photoStorage, never()).store(any(), any());
		}

		/** An HTML or SVG file served back from our own origin could run script. */
		@ParameterizedTest
		@ValueSource(strings = { "image/svg+xml", "text/html", "application/pdf", "application/octet-stream" })
		void shouldRefuseATypeThatIsNotAPlainImage(String contentType) {
			assertThatThrownBy(() -> photoService.upload(member, LocalDate.now(), PhotoPose.ON, contentType,
					JPEG, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("JPEG, PNG veya WebP");
			verify(photoStorage, never()).store(any(), any());
		}

		@Test
		void shouldAcceptATypeWhateverItsCasing() {
			when(photoStorage.store(any(), any())).thenReturn("ab/cd/key.png");
			when(photoRepository.save(any())).thenAnswer(call -> call.getArgument(0));

			var response = photoService.upload(member, LocalDate.now(), PhotoPose.YAN, "IMAGE/PNG", JPEG, null);

			assertThat(response.contentType()).isEqualTo("image/png");
		}

		@Test
		void shouldRefuseAFileOverTheLimit() {
			byte[] tooBig = new byte[(int) MAX_BYTES + 1];

			assertThatThrownBy(() -> photoService.upload(member, LocalDate.now(), PhotoPose.ON, "image/jpeg",
					tooBig, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("en fazla 1 MB");
			verify(photoStorage, never()).store(any(), any());
		}

		@Test
		void shouldRefuseAnEmptyFile() {
			assertThatThrownBy(() -> photoService.upload(member, LocalDate.now(), PhotoPose.ON, "image/jpeg",
					new byte[0], null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("boş");
		}

		@Test
		void shouldRefuseADateInTheFuture() {
			assertThatThrownBy(() -> photoService.upload(member, LocalDate.now().plusDays(1), PhotoPose.ON,
					"image/jpeg", JPEG, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("gelecekte olamaz");
			verify(photoStorage, never()).store(any(), any());
		}

		/** Without a cap one member could fill the disk for everyone. */
		@Test
		void shouldRefuseOnceTheMemberIsAtTheirLimit() {
			when(photoRepository.countByMemberId(member.getId())).thenReturn((long) MAX_PER_MEMBER);

			assertThatThrownBy(() -> photoService.upload(member, LocalDate.now(), PhotoPose.ON, "image/jpeg",
					JPEG, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("En fazla 3 fotoğraf");
			verify(photoStorage, never()).store(any(), any());
		}
	}

	@Nested
	class ReadingAndDeleting {

		@Test
		void shouldAnswerNotFoundForAnotherMembersPhoto() {
			ProgressPhoto theirs = photoOf(member("baskasi@example.com"));
			when(photoRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

			// Not 403: a different answer would confirm the id exists.
			assertThatThrownBy(() -> photoService.readOwn(member, theirs.getId()))
					.isInstanceOf(NotFoundException.class);
			verify(photoStorage, never()).read(any());
			verify(photoRepository, never()).delete(any());
		}

		@Test
		void shouldReadTheMembersOwnPhoto() {
			ProgressPhoto mine = photoOf(member, "ab/cd/key.jpg");
			when(photoRepository.findById(mine.getId())).thenReturn(Optional.of(mine));
			when(photoStorage.read("ab/cd/key.jpg")).thenReturn(new ByteArrayInputStream(JPEG));

			var stored = photoService.readOwn(member, mine.getId());

			assertThat(stored.contentType()).isEqualTo("image/jpeg");
			assertThat(stored.sizeBytes()).isEqualTo(3);
		}

		@Test
		void shouldDeleteTheFileBeforeTheRow() {
			ProgressPhoto mine = photoOf(member, "ab/cd/key.jpg");
			when(photoRepository.findById(mine.getId())).thenReturn(Optional.of(mine));

			photoService.deleteAsCoach(mine.getId());

			var order = org.mockito.Mockito.inOrder(photoStorage, photoRepository);
			order.verify(photoStorage).delete("ab/cd/key.jpg");
			order.verify(photoRepository).delete(mine);
		}

		/**
		 * A photo the member cannot delete because of a storage fault is worse than a key left
		 * behind, which costs only disk.
		 */
		@Test
		void shouldStillDeleteTheRowWhenTheFileCannotBeRemoved() {
			ProgressPhoto mine = photoOf(member);
			when(photoRepository.findById(mine.getId())).thenReturn(Optional.of(mine));
			doThrow(new PhotoStorageException("disk dolu")).when(photoStorage).delete(any());

			assertThatCode(() -> photoService.deleteAsCoach(mine.getId())).doesNotThrowAnyException();
			verify(photoRepository).delete(mine);
		}

		@Test
		void shouldRemoveEveryFileWhenTheMemberIsDeleted() {
			ProgressPhoto first = photoOf(member);
			ProgressPhoto second = photoOf(member);
			when(photoRepository.findAllByMemberIdOrderByTakenOnDesc(member.getId()))
					.thenReturn(List.of(first, second));

			photoService.deleteAllOfMember(member.getId());

			// Body photos must not outlive the membership.
			verify(photoStorage).delete(first.getStorageKey());
			verify(photoStorage).delete(second.getStorageKey());
			verify(photoRepository).deleteAllByMemberId(member.getId());
		}
	}
}
