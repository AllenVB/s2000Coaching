package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MonthlyRecordService;
import com.s2000coaching.web.dto.MonthlyRecordResponse;
import java.math.BigDecimal;
import java.time.Instant;
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

@WebMvcTest({ MemberMonthlyRecordController.class, AdminMonthlyRecordController.class })
class MonthlyRecordControllersTest {

	private static final String MEMBER = "Bearer member-token";
	private static final String COACH = "Bearer coach-token";
	private static final String BODY = "{\"period\":\"2026-10\",\"heightCm\":184,\"weightKg\":80.4,\"waistCm\":86}";

	@Autowired
	private MockMvcTester mvc;
	@MockitoBean
	private MonthlyRecordService recordService;
	@MockitoBean
	private MemberAuthService memberAuthService;
	@MockitoBean
	private CoachAuthService coachAuthService;

	private final Member member = mock(Member.class);
	private final UUID memberId = UUID.randomUUID();

	@BeforeEach
	void sessions() {
		when(member.getId()).thenReturn(memberId);
		when(memberAuthService.requireMember(MEMBER)).thenReturn(member);
		when(memberAuthService.requireMember(null)).thenThrow(new UnauthorizedException("Oturum yok."));
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(COACH);
	}

	private static MonthlyRecordResponse response() {
		return new MonthlyRecordResponse(UUID.randomUUID(), "2026-10", new BigDecimal("184"), new BigDecimal("80.4"),
				null, null, null, null, new BigDecimal("86"), null, null, Instant.now());
	}

	@Test
	void shouldListTheSignedInMembersMonthsOnly() {
		when(recordService.list(memberId)).thenReturn(List.of(response()));

		assertThat(mvc.get().uri("/api/member/monthly-records").header(HttpHeaders.AUTHORIZATION, MEMBER))
				.hasStatus(HttpStatus.OK).bodyJson().extractingPath("$[0].period").isEqualTo("2026-10");
	}

	@Test
	void shouldCreateAMonthFromTheTable() {
		when(recordService.create(any(), any())).thenReturn(response());

		assertThat(mvc.post().uri("/api/member/monthly-records").header(HttpHeaders.AUTHORIZATION, MEMBER)
				.contentType(MediaType.APPLICATION_JSON).content(BODY)).hasStatus(HttpStatus.CREATED);
		verify(recordService).create(any(), any());
	}

	@Test
	void shouldRefuseImpossibleFiguresAndABadMonthBeforeAnythingIsSaved() {
		for (String bad : new String[] { "{\"period\":\"2026-10\",\"weightKg\":8}", "{\"period\":\"2026-13\",\"weightKg\":80}",
				"{\"period\":\"2026-10\",\"waistCm\":800}" }) {
			assertThat(mvc.post().uri("/api/member/monthly-records").header(HttpHeaders.AUTHORIZATION, MEMBER)
					.contentType(MediaType.APPLICATION_JSON).content(bad)).hasStatus(HttpStatus.BAD_REQUEST);
		}
		verify(recordService, never()).create(any(), any());
	}

	@Test
	void shouldRefuseEverythingWithoutASession() {
		assertThat(mvc.get().uri("/api/member/monthly-records")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.delete().uri("/api/member/monthly-records/" + UUID.randomUUID())).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldEditAndDeleteByIdAsTheMember() {
		UUID id = UUID.randomUUID();
		when(recordService.update(any(), any(), any())).thenReturn(response());

		assertThat(mvc.put().uri("/api/member/monthly-records/" + id).header(HttpHeaders.AUTHORIZATION, MEMBER)
				.contentType(MediaType.APPLICATION_JSON).content(BODY)).hasStatus(HttpStatus.OK);
		assertThat(mvc.delete().uri("/api/member/monthly-records/" + id).header(HttpHeaders.AUTHORIZATION, MEMBER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(recordService).delete(memberId, id);
	}

	@Test
	void shouldLetTheCoachListCreateEditAndDeleteForAMember() {
		UUID id = UUID.randomUUID();
		when(recordService.listAsCoach(memberId)).thenReturn(List.of(response()));
		when(recordService.create(any(), any())).thenReturn(response());
		when(recordService.updateAsCoach(any(), any())).thenReturn(response());

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/monthly-records").header(HttpHeaders.AUTHORIZATION, COACH))
				.hasStatus(HttpStatus.OK);
		assertThat(mvc.post().uri("/api/admin/members/" + memberId + "/monthly-records")
				.header(HttpHeaders.AUTHORIZATION, COACH).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.hasStatus(HttpStatus.CREATED);
		assertThat(mvc.put().uri("/api/admin/monthly-records/" + id).header(HttpHeaders.AUTHORIZATION, COACH)
				.contentType(MediaType.APPLICATION_JSON).content(BODY)).hasStatus(HttpStatus.OK);
		assertThat(mvc.delete().uri("/api/admin/monthly-records/" + id).header(HttpHeaders.AUTHORIZATION, COACH))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(recordService).deleteAsCoach(id);
	}

	@Test
	void shouldKeepTheCoachEndpointsBehindTheCoachSession() {
		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/monthly-records")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/monthly-records")
				.header(HttpHeaders.AUTHORIZATION, MEMBER)).hasStatus(HttpStatus.UNAUTHORIZED);
	}
}
