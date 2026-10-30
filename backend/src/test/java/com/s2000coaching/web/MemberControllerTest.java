package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberService;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.UpdateOwnProfileRequest;
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

@WebMvcTest(MemberController.class)
class MemberControllerTest {

	private static final String BEARER = "Bearer token-123";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	private static Member member() {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		member.activate(Instant.now());
		return member;
	}

	private String json(Object body) {
		return jsonMapper.writeValueAsString(body);
	}

	@Test
	void shouldUpdateTheSignedInMemberRatherThanAnyIdTheBodyCarries() {
		Member member = member();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		var request = new UpdateOwnProfileRequest(30, "+905559998877", 183, new BigDecimal("85.00"), "7 yıl fitness");
		when(memberService.updateOwnProfile(eq(member.getId()), any()))
				.thenReturn(MemberResponse.from(member));

		assertThat(mvc.patch().uri("/api/member/me")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(request)))
				.hasStatus(HttpStatus.OK);

		// The member id comes from the token, so one member cannot edit another.
		verify(memberService).updateOwnProfile(eq(member.getId()), any());
	}

	@Test
	void shouldRejectAnUpdateWithoutASession() {
		when(memberAuthService.requireMember(any()))
				.thenThrow(new UnauthorizedException("Oturum bulunamadı."));

		assertThat(mvc.patch().uri("/api/member/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(new UpdateOwnProfileRequest(30, "+905551112233", 182, new BigDecimal("85.00"), "fitness"))))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberService, never()).updateOwnProfile(any(), any());
	}

	@Test
	void shouldRejectAnImpossibleHeight() {
		when(memberAuthService.requireMember(BEARER)).thenReturn(member());

		assertThat(mvc.patch().uri("/api/member/me")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(new UpdateOwnProfileRequest(30, "+905551112233", 20, new BigDecimal("85.00"), "fitness"))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(memberService, never()).updateOwnProfile(any(), any());
	}

	@Test
	void shouldRejectAnImpossibleWeight() {
		when(memberAuthService.requireMember(BEARER)).thenReturn(member());

		assertThat(mvc.patch().uri("/api/member/me")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(new UpdateOwnProfileRequest(30, "+905551112233", 182, new BigDecimal("4.0"), "fitness"))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(memberService, never()).updateOwnProfile(any(), any());
	}
}
