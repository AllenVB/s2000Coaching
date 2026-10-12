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
import com.s2000coaching.service.MeasurementService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.web.dto.MeasurementResponse;
import com.s2000coaching.web.dto.UpsertMeasurementRequest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(MemberMeasurementController.class)
class MemberMeasurementControllerTest {

	private static final String BEARER = "Bearer token-123";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private MeasurementService measurementService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	private static Member member() {
		Member member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233",
				182, new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
		member.activate(Instant.now());
		member.grantHealthDataConsent(Instant.now());
		return member;
	}

	private static UpsertMeasurementRequest request(LocalDate measuredOn) {
		return new UpsertMeasurementRequest(measuredOn, new BigDecimal("80.0"), null, null, null, null, null,
				null);
	}

	@Test
	void shouldReturn401WithoutASession() {
		when(memberAuthService.requireMember(null)).thenThrow(new UnauthorizedException("Oturum bulunamadı."));

		assertThat(mvc.get().uri("/api/member/measurements")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(measurementService, never()).listForMember(any());
	}

	@Test
	void shouldSaveACheckInForTheSignedInMember() {
		Member member = member();
		LocalDate today = LocalDate.now();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);
		when(measurementService.save(eq(member), any())).thenReturn(new MeasurementResponse(
				UUID.randomUUID(), today, new BigDecimal("80.0"), null, null, null, null, null, null,
				Instant.now()));

		assertThat(mvc.post().uri("/api/member/measurements")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request(today))))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.weightKg").isEqualTo(80.0);
	}

	@Test
	void shouldReturn400ForAFutureDate() {
		when(memberAuthService.requireMember(BEARER)).thenReturn(member());

		assertThat(mvc.post().uri("/api/member/measurements")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request(LocalDate.now().plusDays(1)))))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(measurementService, never()).save(any(), any());
	}

	@Test
	void shouldReturn400ForAnImpossibleWeight() {
		when(memberAuthService.requireMember(BEARER)).thenReturn(member());
		var invalid = new UpsertMeasurementRequest(LocalDate.now(), new BigDecimal("5.0"), null, null, null,
				null, null, null);

		assertThat(mvc.post().uri("/api/member/measurements")
				.header(HttpHeaders.AUTHORIZATION, BEARER)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(invalid)))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(measurementService, never()).save(any(), any());
	}

	@Test
	void shouldReturn204WhenDeletingOwnRow() {
		Member member = member();
		UUID measurementId = UUID.randomUUID();
		when(memberAuthService.requireMember(BEARER)).thenReturn(member);

		assertThat(mvc.delete().uri("/api/member/measurements/" + measurementId)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(measurementService).deleteOwn(member, measurementId);
	}
}
