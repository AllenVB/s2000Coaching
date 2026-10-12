package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.ProgressPhotoService;
import com.s2000coaching.web.dto.ProgressPhotoResponse;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AdminPhotoController.class)
class AdminPhotoControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ProgressPhotoService photoService;

	/** The /api/admin/** interceptor resolves the coach session through this. */
	@MockitoBean
	private CoachAuthService coachAuthService;

	@BeforeEach
	void stubCoachSession() {
		// doThrow/doReturn rather than when(...): a second when() would itself call the
		// already-stubbed method and trip the exception during setup.
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(BEARER);
	}

	@Test
	void shouldRejectReadsWithoutACoachSession() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/photos"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(photoService, never()).listForMemberAsCoach(any());
	}

	@Test
	void shouldListAMembersPhotos() {
		UUID memberId = UUID.randomUUID();
		when(photoService.listForMemberAsCoach(memberId)).thenReturn(List.of(new ProgressPhotoResponse(
				UUID.randomUUID(), LocalDate.now(), PhotoPose.ARKA, "image/png", 42, "not", Instant.now())));

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/photos")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$[0].pose").isEqualTo("ARKA");
	}

	@Test
	void shouldServeTheImageBytes() {
		UUID photoId = UUID.randomUUID();
		when(photoService.readAsCoach(photoId)).thenReturn(new ProgressPhotoService.StoredPhoto(
				new ByteArrayInputStream(new byte[] { 1, 2 }), "image/png", 2));

		assertThat(mvc.get().uri("/api/admin/photos/" + photoId + "/content")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.hasContentType(MediaType.IMAGE_PNG)
				.hasHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");
	}

	@Test
	void shouldReturn204WhenDeleting() {
		UUID photoId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/photos/" + photoId)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(photoService).deleteAsCoach(photoId);
	}

	/** A body photo is the member's to put there; the coach reads and removes, never uploads. */
	@Test
	void shouldNotExposeAnUploadRoute() {
		assertThat(mvc.post().uri("/api/admin/members/" + UUID.randomUUID() + "/photos")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
	}
}
