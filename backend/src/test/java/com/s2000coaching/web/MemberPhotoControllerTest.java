package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.PhotoPose;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.ProgressPhotoService;
import com.s2000coaching.web.dto.ProgressPhotoResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(MemberPhotoController.class)
class MemberPhotoControllerTest {

	private static final String BEARER = "Bearer token-123";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ProgressPhotoService photoService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	private static Member member() {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
		member.grantHealthDataConsent(Instant.now());
		return member;
	}

	private static MockMultipartFile file() {
		return new MockMultipartFile("file", "on.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] { 1, 2, 3 });
	}

	@Test
	void shouldUploadForTheSignedInMember() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(photoService.upload(eq(member), any(), any(), any(), any(), any()))
				.thenReturn(new ProgressPhotoResponse(UUID.randomUUID(), LocalDate.now(), PhotoPose.ON,
						"image/jpeg", 3, null, Instant.now()));

		assertThat(mvc.post().multipart().uri("/api/member/photos")
				.file(file())
				.param("takenOn", LocalDate.now().toString())
				.param("pose", "ON")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK);

		// The owner comes from the token, never from the form.
		verify(photoService).upload(eq(member), any(), eq(PhotoPose.ON), eq("image/jpeg"), any(), any());
	}

	@Test
	void shouldRejectAnUploadWithoutASession() {
		when(memberAuthService.requireMember(any()))
				.thenThrow(new UnauthorizedException("Oturum bulunamadı."));

		assertThat(mvc.post().multipart().uri("/api/member/photos")
				.file(file())
				.param("takenOn", LocalDate.now().toString())
				.param("pose", "ON"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(photoService, never()).upload(any(), any(), any(), any(), any(), any());
	}

	@Test
	void shouldServeTheImageBytesWithAPrivateCacheHeader() {
		Member member = member();
		UUID photoId = UUID.randomUUID();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(photoService.readOwn(member, photoId)).thenReturn(new ProgressPhotoService.StoredPhoto(
				new ByteArrayInputStream(new byte[] { 1, 2, 3 }), "image/jpeg", 3));

		assertThat(mvc.get().uri("/api/member/photos/" + photoId + "/content")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.hasContentType(MediaType.IMAGE_JPEG)
				// Special-category data: a shared proxy must not keep a copy.
				.hasHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");
	}

	@Test
	void shouldRejectReadingBytesWithoutASession() {
		when(memberAuthService.requireMember(any()))
				.thenThrow(new UnauthorizedException("Oturum bulunamadı."));

		assertThat(mvc.get().uri("/api/member/photos/" + UUID.randomUUID() + "/content"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(photoService, never()).readOwn(any(), any());
	}

	@Test
	void shouldReturn204WhenDeletingOwnPhoto() {
		Member member = member();
		UUID photoId = UUID.randomUUID();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);

		assertThat(mvc.delete().uri("/api/member/photos/" + photoId)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(photoService).deleteOwn(member, photoId);
	}
}
