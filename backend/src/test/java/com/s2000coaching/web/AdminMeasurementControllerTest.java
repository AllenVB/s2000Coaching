package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.MeasurementService;
import com.s2000coaching.web.dto.MeasurementResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AdminMeasurementController.class)
@TestPropertySource(properties = "app.admin.token=test-admin-token")
class AdminMeasurementControllerTest {

	private static final String ADMIN_HEADER = "X-Admin-Token";
	private static final String ADMIN_TOKEN = "test-admin-token";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private MeasurementService measurementService;

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
				.header(ADMIN_HEADER, ADMIN_TOKEN))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$[0].weightKg").isEqualTo(80.0);
	}

	@Test
	void shouldReturn204WhenDeleting() {
		UUID measurementId = UUID.randomUUID();

		assertThat(mvc.delete().uri("/api/admin/measurements/" + measurementId)
				.header(ADMIN_HEADER, ADMIN_TOKEN))
				.hasStatus(HttpStatus.NO_CONTENT);
		verify(measurementService).deleteAsCoach(measurementId);
	}
}
