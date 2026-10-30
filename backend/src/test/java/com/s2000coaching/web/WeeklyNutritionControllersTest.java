package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.WeeklyNutritionService;
import com.s2000coaching.web.dto.WeeklyNutritionResponse;
import com.s2000coaching.web.dto.WeeklyNutritionResponse.Macros;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

@WebMvcTest({ MemberWeeklyNutritionController.class, AdminWeeklyNutritionController.class })
class WeeklyNutritionControllersTest {

	private static final String MEMBER = "Bearer member-token";
	private static final String COACH = "Bearer coach-token";
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

	@Autowired
	private MockMvcTester mvc;
	@MockitoBean
	private WeeklyNutritionService service;
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

	private static WeeklyNutritionResponse response() {
		Macros weekly = new Macros(new BigDecimal("14000.0"), new BigDecimal("1750.0"), new BigDecimal("466.7"),
				new BigDecimal("700.0"));
		Macros daily = new Macros(new BigDecimal("2000.0"), new BigDecimal("250.0"), new BigDecimal("66.7"),
				new BigDecimal("100.0"));
		return new WeeklyNutritionResponse(UUID.randomUUID(), MONDAY, MONDAY.plusDays(6), 14_000, 50, 30, 20, weekly,
				daily, true, Instant.now(), Instant.now());
	}

	private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder put() {
		return multipart(HttpMethod.PUT, "/api/member/weekly-nutrition")
				.param("weekStart", "2026-10-26").param("calories", "14000")
				.param("carbsPercent", "50").param("fatPercent", "30").param("proteinPercent", "20");
	}

	@Test
	void shouldListTheSignedInMembersWeeksWithTheCalculatedMacros() {
		when(service.list(memberId)).thenReturn(List.of(response()));

		var result = mvc.get().uri("/api/member/weekly-nutrition").header(HttpHeaders.AUTHORIZATION, MEMBER);

		assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("$[0].dailyAverage.carbsG").isEqualTo(250.0);
		assertThat(result).bodyJson().extractingPath("$[0].weekEnd").isEqualTo("2026-11-01");
	}

	@Test
	void shouldSaveAWeekWithAPhoto() {
		when(service.save(eq(member), eq(MONDAY), eq(14_000), eq(50), eq(30), eq(20), any(), eq(false)))
				.thenReturn(response());

		var result = mvc.perform(put().file(new MockMultipartFile("photo", "yemek.jpg", "image/jpeg", new byte[] { 1, 2 }))
				.header(HttpHeaders.AUTHORIZATION, MEMBER));

		assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("$.hasPhoto").isEqualTo(true);
	}

	@Test
	void shouldSaveAWeekWithoutAPhoto() {
		when(service.save(eq(member), eq(MONDAY), anyInt(), anyInt(), anyInt(), anyInt(), isNull(), anyBoolean()))
				.thenReturn(response());

		assertThat(mvc.perform(put().header(HttpHeaders.AUTHORIZATION, MEMBER))).hasStatus(HttpStatus.OK);
	}

	@Test
	void shouldAnswer400WhenTheServiceRefusesTheFigures() {
		when(service.save(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyBoolean()))
				.thenThrow(new BadRequestException("Karbonhidrat, yağ ve protein yüzdelerinin toplamı 100 olmalı (şu an 90)."));

		assertThat(mvc.perform(put().header(HttpHeaders.AUTHORIZATION, MEMBER))).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void shouldAnswer400WhenAFigureIsMissing() {
		assertThat(mvc.perform(multipart(HttpMethod.PUT, "/api/member/weekly-nutrition")
				.param("weekStart", "2026-10-26").param("calories", "14000").header(HttpHeaders.AUTHORIZATION, MEMBER)))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(service, never()).save(any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyBoolean());
	}

	@Test
	void shouldRequireASession() {
		assertThat(mvc.get().uri("/api/member/weekly-nutrition")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/weekly-nutrition")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldServeThePhotoPrivately() {
		UUID id = UUID.randomUUID();
		when(service.readOwnPhoto(member, id)).thenReturn(
				new WeeklyNutritionService.StoredPhoto(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), "image/jpeg", 3));

		var result = mvc.get().uri("/api/member/weekly-nutrition/" + id + "/photo").header(HttpHeaders.AUTHORIZATION, MEMBER);

		assertThat(result).hasStatus(HttpStatus.OK).hasHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");
	}

	@Test
	void shouldLetTheCoachReadAMembersWeeksAndRemoveOne() {
		UUID id = UUID.randomUUID();
		when(service.listAsCoach(memberId)).thenReturn(List.of(response()));

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/weekly-nutrition").header(HttpHeaders.AUTHORIZATION, COACH))
				.hasStatus(HttpStatus.OK);
		assertThat(mvc.delete().uri("/api/admin/weekly-nutrition/" + id).header(HttpHeaders.AUTHORIZATION, COACH))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(service).deleteAsCoach(id);
	}

	@Test
	void shouldDeleteTheMembersOwnWeek() {
		UUID id = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/member/weekly-nutrition/" + id).header(HttpHeaders.AUTHORIZATION, MEMBER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(service).deleteOwn(member, id);
	}
}
