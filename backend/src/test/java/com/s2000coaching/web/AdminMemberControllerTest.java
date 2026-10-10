package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.MemberService;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.VerificationCodeRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AdminMemberController.class)
@TestPropertySource(properties = "app.admin.token=test-admin-token")
class AdminMemberControllerTest {

	private static final String ADMIN_HEADER = "X-Admin-Token";
	private static final String ADMIN_TOKEN = "test-admin-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MemberService memberService;

	private static CreateMemberRequest validRequest() {
		return new CreateMemberRequest("sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness");
	}

	private static MemberResponse response(UUID id, MemberStatus status) {
		return new MemberResponse(id, "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", status, Instant.now(),
				status == MemberStatus.ACTIVE ? Instant.now() : null);
	}

	@Test
	void shouldRejectRequestsWithoutTheAdminToken() {
		assertThat(mvc.get().uri("/api/admin/members")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberService, never()).listMembers();
	}

	@Test
	void shouldRejectRequestsWithAWrongAdminToken() {
		assertThat(mvc.post().uri("/api/admin/members")
				.header(ADMIN_HEADER, "wrong")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(validRequest())))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberService, never()).createMember(any());
	}

	@Test
	void shouldCreateMemberAsPendingVerification() {
		when(memberService.createMember(any())).thenReturn(response(UUID.randomUUID(),
				MemberStatus.PENDING_VERIFICATION));

		assertThat(mvc.post().uri("/api/admin/members")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(validRequest())))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.status").isEqualTo("PENDING_VERIFICATION");
	}

	@Test
	void shouldReturn400WhenMeasurementsAreOutOfRange() {
		var invalid = new CreateMemberRequest("sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 40,
				new BigDecimal("4.0"), "5 yıl fitness");

		assertThat(mvc.post().uri("/api/admin/members")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(invalid)))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(memberService, never()).createMember(any());
	}

	@Test
	void shouldCompleteRegistrationWithACode() {
		UUID memberId = UUID.randomUUID();
		when(memberService.completeRegistration(eq(memberId), eq("123456")))
				.thenReturn(response(memberId, MemberStatus.ACTIVE));

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/verification")
				.header(ADMIN_HEADER, ADMIN_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new VerificationCodeRequest("123456"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.status").isEqualTo("ACTIVE");
	}

	@Test
	void shouldReturn204WhenResendingACodeAndWhenDeleting() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/verification-code")
				.header(ADMIN_HEADER, ADMIN_TOKEN))
				.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.delete().uri("/api/admin/members/" + memberId).header(ADMIN_HEADER, ADMIN_TOKEN))
				.hasStatus(HttpStatus.NO_CONTENT);

		verify(memberService).resendRegistrationCode(memberId);
		verify(memberService).deleteMember(memberId);
	}
}
