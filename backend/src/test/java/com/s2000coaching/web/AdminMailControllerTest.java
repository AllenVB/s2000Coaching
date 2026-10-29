package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.mail.MailDelivery;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.MailTestService;
import com.s2000coaching.web.dto.MailTestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AdminMailController.class)
class AdminMailControllerTest {

	private static final String BEARER = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private MailTestService mailTestService;

	@MockitoBean
	private CoachAuthService coachAuthService;

	private final CoachAccount coach = mock(CoachAccount.class);

	@BeforeEach
	void stubCoachSession() {
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(coach).when(coachAuthService).requireCoach(BEARER);
	}

	@Test
	void shouldRefuseATestMailWithoutASession() {
		assertThat(mvc.post().uri("/api/admin/mail/test")).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(mailTestService, never()).sendTo(any());
	}

	@Test
	void shouldSendTheTestMailToTheSignedInCoachAndReportTheOutcome() {
		when(mailTestService.sendTo(coach)).thenReturn(
				new MailTestResponse(MailDelivery.FAILED, "koc@example.com", "Authentication failed"));

		assertThat(mvc.post().uri("/api/admin/mail/test").header(HttpHeaders.AUTHORIZATION, BEARER))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.detail").isEqualTo("Authentication failed");
	}
}
