package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberService;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.domain.MemberGender;
import com.s2000coaching.mail.MailDelivery;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.CreateMemberResponse;
import com.s2000coaching.web.dto.MemberResponse;
import com.s2000coaching.web.dto.UpdateMemberRequest;
import com.s2000coaching.web.dto.VerificationCodeRequest;
import java.math.BigDecimal;
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

@WebMvcTest(AdminMemberController.class)
class AdminMemberControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	/** The /api/admin/** interceptor resolves the coach session through this. */
	@MockitoBean
	private CoachAuthService coachAuthService;

	@BeforeEach
	void stubCoachSession() {
		// The interceptor only cares whether this throws, so the valid token resolves to
		// nothing in particular and everything else is rejected — as the real one does.
		// doThrow/doReturn rather than when(...): a second when() would itself call the
		// already-stubbed method and trip the exception during setup.
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(BEARER);
	}

	private static CreateMemberRequest validRequest() {
		return new CreateMemberRequest("sporcu@example.com", "Ahmet Yılmaz", "+905551112233", null, null,
				182, new BigDecimal("84.50"), "5 yıl fitness");
	}

	private static MemberResponse response(UUID id, MemberStatus status) {
		return new MemberResponse(id, "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", status, Instant.now(),
				status == MemberStatus.ACTIVE ? Instant.now() : null, null, true, MemberGender.MALE, 25, true, 12345);
	}

	@Test
	void shouldRejectRequestsWithoutTheAdminToken() {
		assertThat(mvc.get().uri("/api/admin/members")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberService, never()).listMembers();
	}

	@Test
	void shouldRejectRequestsWithAWrongAdminToken() {
		assertThat(mvc.post().uri("/api/admin/members")
				.header(HttpHeaders.AUTHORIZATION, "Bearer wrong")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(validRequest())))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberService, never()).createMember(any());
	}

	@Test
	void shouldCreateMemberAsPendingVerification() {
		when(memberService.createMember(any())).thenReturn(new CreateMemberResponse(
				response(UUID.randomUUID(), MemberStatus.PENDING_VERIFICATION), MailDelivery.SENT));

		assertThat(mvc.post().uri("/api/admin/members")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(validRequest())))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.member.status").isEqualTo("PENDING_VERIFICATION");
	}

	/** The member exists either way; the coach has to be able to see that the invitation did not go. */
	@Test
	void shouldTellTheCoachWhenTheInvitationMailFailed() {
		when(memberService.createMember(any())).thenReturn(new CreateMemberResponse(
				response(UUID.randomUUID(), MemberStatus.PENDING_VERIFICATION), MailDelivery.FAILED));

		assertThat(mvc.post().uri("/api/admin/members")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(validRequest())))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.inviteDelivery").isEqualTo("FAILED");
	}

	@Test
	void shouldReturn400WhenMeasurementsAreOutOfRange() {
		var invalid = new CreateMemberRequest("sporcu@example.com", "Ahmet Yılmaz", "+905551112233", null, null,
				40, new BigDecimal("4.0"), "5 yıl fitness");

		assertThat(mvc.post().uri("/api/admin/members")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
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
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(new VerificationCodeRequest("123456"))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.status").isEqualTo("ACTIVE");
	}

	@Test
	void shouldReturnASingleMember() {
		UUID memberId = UUID.randomUUID();
		when(memberService.getMember(memberId)).thenReturn(response(memberId, MemberStatus.ACTIVE));

		assertThat(mvc.get().uri("/api/admin/members/" + memberId).header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.email").isEqualTo("sporcu@example.com");
	}

	@Test
	void shouldUpdateAMember() {
		UUID memberId = UUID.randomUUID();
		var request = new UpdateMemberRequest("mehmet@example.com", "Mehmet Demir", MemberGender.MALE, 30,
				"+905559998877", 175, new BigDecimal("79.00"), "Yüzme");
		when(memberService.updateMember(eq(memberId), any())).thenReturn(response(memberId, MemberStatus.ACTIVE));

		assertThat(mvc.put().uri("/api/admin/members/" + memberId)
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.hasStatus(HttpStatus.OK);
		verify(memberService).updateMember(eq(memberId), any());
	}

	@Test
	void shouldRejectAnUpdateWithoutTheAdminToken() {
		UUID memberId = UUID.randomUUID();
		var request = new UpdateMemberRequest("mehmet@example.com", "Mehmet Demir", MemberGender.MALE, 30,
				"+905559998877", 175, new BigDecimal("79.00"), "Yüzme");

		assertThat(mvc.put().uri("/api/admin/members/" + memberId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberService, never()).updateMember(any(), any());
	}

	@Test
	void shouldReportWhetherAResentCodeWasMailed() {
		UUID memberId = UUID.randomUUID();
		when(memberService.resendRegistrationCode(memberId)).thenReturn(MailDelivery.FAILED);

		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/verification-code")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.delivery").isEqualTo("FAILED");
	}

	@Test
	void shouldReturn204WhenDeleting() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/members/" + memberId).header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);

		verify(memberService).deleteMember(memberId);
	}

	@Test
	void shouldLetTheCoachSetAMembersPassword() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.put().uri("/api/admin/members/" + memberId + "/password")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"newPassword\":\"Koc-verdi-1\"}"))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(memberAuthService).setPasswordAsCoach(memberId, "Koc-verdi-1");
	}

	@Test
	void shouldRefuseAWeakPasswordAndAMissingToken() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.put().uri("/api/admin/members/" + memberId + "/password")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"newPassword\":\"kisa\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.put().uri("/api/admin/members/" + memberId + "/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"newPassword\":\"Koc-verdi-1\"}")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(memberAuthService, never()).setPasswordAsCoach(any(), any());
	}
}
