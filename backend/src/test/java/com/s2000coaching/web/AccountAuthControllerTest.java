package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.s2000coaching.exception.EmailNotVerifiedException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.AccountAuthService;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.AccountSessionResponse;
import com.s2000coaching.web.dto.CoachSessionResponse;
import com.s2000coaching.web.dto.MemberLoginRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AccountAuthController.class)
class AccountAuthControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private AccountAuthService accountAuthService;

	// The interceptor that guards /api/admin/** is built from this bean.
	@MockitoBean
	private CoachAuthService coachAuthService;

	private String body(String password) {
		return jsonMapper.writeValueAsString(new MemberLoginRequest("kisi@example.com", password));
	}

	@Test
	void shouldAnswerWithTheRoleAndThatSidesSession() {
		when(accountAuthService.login(eq("kisi@example.com"), eq("sifre")))
				.thenReturn(AccountSessionResponse.ofCoach(new CoachSessionResponse("t-1", Instant.now(), null)));

		var result = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("sifre"));

		assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("$.role").isEqualTo("COACH");
		assertThat(result).bodyJson().extractingPath("$.coach.token").isEqualTo("t-1");
	}

	@Test
	void shouldReturn401ForWrongCredentials() {
		when(accountAuthService.login(any(), any())).thenThrow(new UnauthorizedException("E-posta veya şifre hatalı."));

		assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("x")))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldReturn403ForAnUnconfirmedMember() {
		when(accountAuthService.login(any(), any())).thenThrow(new EmailNotVerifiedException("Doğrulayın."));

		assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("x")))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldRejectAMissingPassword() {
		assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"kisi@example.com\"}")).hasStatus(HttpStatus.BAD_REQUEST);
	}
}
