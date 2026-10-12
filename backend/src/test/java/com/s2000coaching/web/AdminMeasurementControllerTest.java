package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.MeasurementService;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.web.dto.MeasurementResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AdminMeasurementController.class)
class AdminMeasurementControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private MeasurementService measurementService;

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

	@Test
	void shouldRejectReadsWithoutTheAdminToken() {
		UUID memberId = UUID.randomUUID();

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/measurements"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
		verify(measurementService, never()).listForMemberAsCoach(any());
	}

	@Test
	void shouldListAMembersCheckIns() {
		UUID memberId = UUID.randomUUID();
		when(measurementService.listForMemberAsCoach(memberId)).thenReturn(List.of(new MeasurementResponse(
				UUID.randomUUID(), LocalDate.now(), new BigDecimal("80.0"), null, null, null, null, null, null,
				Instant.now())));

		assertThat(mvc.get().uri("/api/admin/members/" + memberId + "/measurements")
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$[0].weightKg").isEqualTo(80.0);
	}

	@Test
	void shouldReturn204WhenDeleting() {
		UUID measurementId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/measurements/" + measurementId)
				.header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(measurementService).deleteAsCoach(measurementId);
	}
}
