package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.CoachLoginRequest;
import com.s2000coaching.web.dto.CoachResponse;
import com.s2000coaching.web.dto.CoachSessionResponse;
import java.time.Instant;
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
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CoachAuthController.class)
class CoachAuthControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private CoachAuthService coachAuthService;

	@BeforeEach
	void stubCoachSession() {
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(BEARER);
	}

	@Test
	void shouldSignInWithTheRightCredentials() {
		var coach = new CoachResponse(UUID.randomUUID(), "koc@example.com", "Koç", null);
		when(coachAuthService.login(eq("koc@example.com"), eq("sifre")))
				.thenReturn(new CoachSessionResponse("token-123", Instant.now().plusSeconds(60), coach));

		assertThat(mvc.post().uri("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new CoachLoginRequest("koc@example.com", "sifre"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.token").isEqualTo("token-123");
	}

	@Test
	void shouldReturn401ForWrongCredentials() {
		when(coachAuthService.login(any(), any()))
				.thenThrow(new UnauthorizedException("E-posta veya şifre hatalı."));

		assertThat(mvc.post().uri("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new CoachLoginRequest("koc@example.com", "yanlis"))))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldReturn400ForAMalformedLogin() {
		assertThat(mvc.post().uri("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new CoachLoginRequest("gecersiz", ""))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(coachAuthService, never()).login(any(), any());
	}

	/** Login is the one admin route the interceptor must let through unauthenticated. */
	@Test
	void shouldNotRequireASessionToReachLogin() {
		when(coachAuthService.login(any(), any())).thenThrow(new UnauthorizedException("hatalı"));

		assertThat(mvc.post().uri("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new CoachLoginRequest("koc@example.com", "x"))))
				// 401 from the service itself, not from the gate refusing the route.
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(coachAuthService).login(any(), any());
	}

	@Test
	void shouldReturn204OnLogout() {
		assertThat(mvc.post().uri("/api/admin/auth/logout").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(coachAuthService).logout(BEARER);
	}
}
