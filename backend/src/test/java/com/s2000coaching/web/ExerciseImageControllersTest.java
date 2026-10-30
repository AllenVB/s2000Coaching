package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.ExerciseService;
import com.s2000coaching.service.ExerciseService.StoredImage;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.ExerciseResponse;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest({ AdminExerciseController.class, MemberExerciseImageController.class })
class ExerciseImageControllersTest {

	private static final String MEMBER = "Bearer member-token";
	private static final String COACH = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;
	@MockitoBean
	private ExerciseService exerciseService;
	@MockitoBean
	private MemberAuthService memberAuthService;
	@MockitoBean
	private CoachAuthService coachAuthService;

	private final UUID exerciseId = UUID.randomUUID();

	@BeforeEach
	void sessions() {
		when(memberAuthService.requireMember(MEMBER)).thenReturn(mock(Member.class));
		when(memberAuthService.requireMember(null)).thenThrow(new UnauthorizedException("Oturum yok."));
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(COACH);
	}

	private StoredImage image() {
		return new StoredImage(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), "image/png", 3);
	}

	@Test
	void shouldServeTheImageToASignedInMemberPrivately() {
		when(exerciseService.readImage(exerciseId)).thenReturn(image());

		var result = mvc.get().uri("/api/member/exercises/" + exerciseId + "/image").header(HttpHeaders.AUTHORIZATION, MEMBER);

		assertThat(result).hasStatus(HttpStatus.OK).hasHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");
	}

	@Test
	void shouldNotServeTheImageWithoutASession() {
		assertThat(mvc.get().uri("/api/member/exercises/" + exerciseId + "/image")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/admin/exercises/" + exerciseId + "/image")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldLetTheCoachUploadReplaceAndRemoveAnImage() {
		var response = new ExerciseResponse(exerciseId, "Squat", null, null, null, null, true, Instant.now());
		when(exerciseService.setImage(any(), any())).thenReturn(response);
		when(exerciseService.removeImage(exerciseId)).thenReturn(response);

		assertThat(mvc.perform(multipart(HttpMethod.PUT, "/api/admin/exercises/" + exerciseId + "/image")
				.file(new MockMultipartFile("file", "squat.png", "image/png", new byte[] { 1, 2 }))
				.header(HttpHeaders.AUTHORIZATION, COACH))).hasStatus(HttpStatus.OK)
				.bodyJson().extractingPath("$.hasImage").isEqualTo(true);
		assertThat(mvc.delete().uri("/api/admin/exercises/" + exerciseId + "/image").header(HttpHeaders.AUTHORIZATION, COACH))
				.hasStatus(HttpStatus.OK);
		verify(exerciseService).removeImage(exerciseId);
	}

	@Test
	void shouldNotLetAMemberUploadAnImage() {
		assertThat(mvc.perform(multipart(HttpMethod.PUT, "/api/admin/exercises/" + exerciseId + "/image")
				.file(new MockMultipartFile("file", "x.png", "image/png", new byte[] { 1 }))
				.header(HttpHeaders.AUTHORIZATION, MEMBER))).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldAnswer400ForAnUploadTheServiceRefuses() {
		when(exerciseService.setImage(any(), any())).thenThrow(new BadRequestException("Fotoğraf işlenemedi."));

		assertThat(mvc.perform(multipart(HttpMethod.PUT, "/api/admin/exercises/" + exerciseId + "/image")
				.file(new MockMultipartFile("file", "x.png", "image/png", new byte[] { 1 }))
				.header(HttpHeaders.AUTHORIZATION, COACH))).hasStatus(HttpStatus.BAD_REQUEST);
	}
}
