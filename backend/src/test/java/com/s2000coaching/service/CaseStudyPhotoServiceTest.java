package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyPhoto;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CaseStudyPhotoRepository;
import com.s2000coaching.storage.PhotoStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseStudyPhotoServiceTest {

	@Mock
	private CaseStudyPhotoRepository photoRepository;

	@Mock
	private PhotoStorage photoStorage;

	@Mock
	private CaseStudyService caseStudyService;

	private CaseStudyPhotoService service;
	private CaseStudy study;

	@BeforeEach
	void setUp() {
		service = new CaseStudyPhotoService(photoRepository, photoStorage, caseStudyService, 1024 * 1024, 3);
		study = new CaseStudy(UUID.randomUUID(), "kendi-verim", "Kendi Verim", Instant.now());
	}

	private static byte[] jpeg() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "jpeg", out);
		return out.toByteArray();
	}

	private CaseStudyPhoto stored() {
		return new CaseStudyPhoto(UUID.randomUUID(), study.getId(), LocalDate.of(2025, 1, 1), PhotoPose.ON,
				"key", "image/jpeg", 5, Instant.now());
	}

	@Test
	void shouldStoreTheCleanedBytesNotTheOriginal() throws Exception {
		when(photoStorage.store(any(), anyString())).thenReturn("key");
		when(photoRepository.save(any())).thenAnswer(call -> call.getArgument(0));
		byte[] original = jpeg();

		service.upload(study.getId(), LocalDate.of(2025, 1, 1), PhotoPose.ON, original);

		var stored = ArgumentCaptor.forClass(byte[].class);
		verify(photoStorage).store(stored.capture(), org.mockito.ArgumentMatchers.eq("image/jpeg"));
		// Re-encoded, so a decodable image that is not byte-identical to what was sent.
		assertThat(ImageIO.read(new ByteArrayInputStream(stored.getValue()))).isNotNull();
		assertThat(stored.getValue()).isNotEqualTo(original);
	}

	@Test
	void shouldStoreNothingWhenTheBytesAreNotAnImage() {
		assertThatThrownBy(() -> service.upload(study.getId(), LocalDate.of(2025, 1, 1), PhotoPose.ON,
				"not an image".getBytes())).isInstanceOf(BadRequestException.class);

		verify(photoStorage, never()).store(any(), anyString());
		verify(photoRepository, never()).save(any());
	}

	@Test
	void shouldRejectAFutureDate() {
		assertThatThrownBy(() -> service.upload(study.getId(), LocalDate.now().plusDays(1), PhotoPose.ON,
				new byte[] {1})).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldRejectAFileOverTheSizeLimit() {
		assertThatThrownBy(() -> service.upload(study.getId(), LocalDate.of(2025, 1, 1), PhotoPose.ON,
				new byte[1024 * 1024 + 1])).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldRejectAnUploadOverThePerStudyLimit() throws Exception {
		when(photoRepository.countByCaseStudyId(study.getId())).thenReturn(3L);

		assertThatThrownBy(() -> service.upload(study.getId(), LocalDate.of(2025, 1, 1), PhotoPose.ON, jpeg()))
				.isInstanceOf(BadRequestException.class);
	}

	// --- the public read: the leak this whole design exists to prevent -----------------------

	@Test
	void shouldNotServeAPhotoPublicWhilePhotosAreSwitchedOff() {
		study.setPublished(true);
		when(caseStudyService.requirePublished("kendi-verim")).thenReturn(study);

		assertThatThrownBy(() -> service.readPublic("kendi-verim", UUID.randomUUID()))
				.isInstanceOf(NotFoundException.class);
		verify(photoStorage, never()).read(anyString());
	}

	@Test
	void shouldNotServeAPhotoOfAnUnpublishedStudy() {
		when(caseStudyService.requirePublished("kendi-verim"))
				.thenThrow(new NotFoundException("Yayınlanmış vaka çalışması bulunamadı."));

		assertThatThrownBy(() -> service.readPublic("kendi-verim", UUID.randomUUID()))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldNotServeAPhotoThatBelongsToAnotherStudy() {
		study.setPublished(true);
		study.setPhotosPublished(true);
		UUID photoId = UUID.randomUUID();
		when(caseStudyService.requirePublished("kendi-verim")).thenReturn(study);
		when(photoRepository.findByIdAndCaseStudyId(photoId, study.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readPublic("kendi-verim", photoId))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldServeAPhotoWhenPublishedAndPhotosAreOn() {
		study.setPublished(true);
		study.setPhotosPublished(true);
		var photo = stored();
		when(caseStudyService.requirePublished("kendi-verim")).thenReturn(study);
		when(photoRepository.findByIdAndCaseStudyId(photo.getId(), study.getId())).thenReturn(Optional.of(photo));
		when(photoStorage.read("key")).thenReturn(new ByteArrayInputStream(new byte[] {1}));

		assertThat(service.readPublic("kendi-verim", photo.getId()).contentType()).isEqualTo("image/jpeg");
	}

	@Test
	void shouldSwitchPhotosOffWhenTheLastPhotoIsDeleted() {
		var photo = stored();
		when(photoRepository.findById(photo.getId())).thenReturn(Optional.of(photo));
		when(photoRepository.countByCaseStudyId(study.getId())).thenReturn(0L);

		service.delete(photo.getId());

		verify(photoStorage).delete("key");
		verify(caseStudyService).setPhotosPublishedUnchecked(study.getId(), false);
	}

	@Test
	void shouldKeepPhotosOnWhileSomeRemain() {
		var photo = stored();
		when(photoRepository.findById(photo.getId())).thenReturn(Optional.of(photo));
		when(photoRepository.countByCaseStudyId(study.getId())).thenReturn(2L);

		service.delete(photo.getId());

		verify(caseStudyService, never()).setPhotosPublishedUnchecked(any(), org.mockito.ArgumentMatchers.anyBoolean());
	}
}
