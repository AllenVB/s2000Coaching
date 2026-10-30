package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberService;
import com.s2000coaching.web.dto.MemberCodeVerificationRequest;
import com.s2000coaching.web.dto.MemberEmailRequest;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.MemberSessionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest({ MemberAuthController.class, MemberController.class })
class MemberAuthControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MemberAuthService memberAuthService;

	/** MemberController records health-data consent through this; unused by these cases. */
	@MockitoBean
	private MemberService memberService;

	private static Member activeMember() {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		member.activate(Instant.now());
		return member;
	}

	@Test
	void shouldAnswer204ForAnyRequestedCode() {
		assertThat(mvc.post().uri("/api/member/auth/request-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new MemberEmailRequest("yok@example.com"))))
				.hasStatus(HttpStatus.NO_CONTENT);

		verify(memberAuthService).requestCode("yok@example.com");
	}

	@Test
	void shouldReturn400ForAMalformedEmail() {
		assertThat(mvc.post().uri("/api/member/auth/request-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new MemberEmailRequest("bad"))))
				.hasStatus(HttpStatus.BAD_REQUEST);

		verify(memberAuthService, never()).requestCode(any());
	}

	@Test
	void shouldReturnASessionForAValidCode() {
		var member = activeMember();
		when(memberAuthService.verifyCode(eq("sporcu@example.com"), eq("123456")))
				.thenReturn(new MemberSessionResponse("token-123", Instant.now().plusSeconds(60),
						MemberResponse.from(member)));

		assertThat(mvc.post().uri("/api/member/auth/verify-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new MemberCodeVerificationRequest("sporcu@example.com", "123456"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.token").isEqualTo("token-123");
	}

	@Test
	void shouldReturn400ForAWrongCode() {
		when(memberAuthService.verifyCode(any(), any())).thenThrow(new BadRequestException("Onay kodu hatalı."));

		assertThat(mvc.post().uri("/api/member/auth/verify-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(
						new MemberCodeVerificationRequest("sporcu@example.com", "000000"))))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void shouldReturnTheSignedInMemberProfile() {
		when(memberAuthService.requireMember("Bearer token-123")).thenReturn(activeMember());

		assertThat(mvc.get().uri("/api/member/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.email").isEqualTo("sporcu@example.com");
	}

	@Test
	void shouldReturn401WithoutASession() {
		when(memberAuthService.requireMember(null))
				.thenThrow(new UnauthorizedException("Oturum bulunamadı. Tekrar giriş yapın."));

		assertThat(mvc.get().uri("/api/member/me")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String signUpJson(String password, boolean acceptedTerms) {
		return jsonMapper.writeValueAsString(new com.s2000coaching.web.dto.RegisterMemberRequest("yeni@example.com",
				password, "Ahmet", "Yılmaz", 27, com.s2000coaching.domain.MemberGender.MALE, 182,
				new java.math.BigDecimal("84.50"), "5 yıl fitness", acceptedTerms));
	}

	@Test
	void shouldOpenAnAccountFromTheSignUpForm() {
		when(memberAuthService.register(org.mockito.ArgumentMatchers.any()))
				.thenReturn(new com.s2000coaching.web.dto.RegistrationResponse(true, null));

		assertThat(mvc.post().uri("/api/member/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(signUpJson("gizli-parola", true)))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson().extractingPath("$.verificationRequired").isEqualTo(true);
	}

	@Test
	void shouldRefuseAShortPasswordAndMissingConsentBeforeAnythingIsCreated() {
		assertThat(mvc.post().uri("/api/member/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(signUpJson("kisa", true))).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.post().uri("/api/member/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(signUpJson("gizli-parola", false))).hasStatus(HttpStatus.BAD_REQUEST);
		org.mockito.Mockito.verify(memberAuthService, org.mockito.Mockito.never()).register(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void shouldAnswer403WhenTheAddressIsNotConfirmedYetSoThePageCanAskForTheCode() {
		when(memberAuthService.login("sporcu@example.com", "gizli-parola"))
				.thenThrow(new com.s2000coaching.exception.EmailNotVerifiedException("Doğrulayın."));

		assertThat(mvc.post().uri("/api/member/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sporcu@example.com\",\"password\":\"gizli-parola\"}"))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldAnswer401ForWrongCredentials() {
		when(memberAuthService.login("sporcu@example.com", "yanlis"))
				.thenThrow(new UnauthorizedException("E-posta veya şifre hatalı."));

		assertThat(mvc.post().uri("/api/member/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sporcu@example.com\",\"password\":\"yanlis\"}"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}
}
