package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.mail.MailDelivery;
import com.s2000coaching.mail.MailResult;
import com.s2000coaching.mail.NotificationMailSender;
import org.junit.jupiter.api.Test;

class MailTestServiceTest {

	private final NotificationMailSender mailSender = mock(NotificationMailSender.class);
	private final MailTestService service = new MailTestService(mailSender);

	private static CoachAccount coach() {
		CoachAccount coach = mock(CoachAccount.class);
		when(coach.getEmail()).thenReturn("koc@example.com");
		return coach;
	}

	@Test
	void shouldMailOnlyTheCoachWhoAsked() {
		when(mailSender.deliver(any(), any(), any())).thenReturn(MailResult.sent());

		var response = service.sendTo(coach());

		verify(mailSender).deliver(eq("koc@example.com"), any(), any());
		assertThat(response.delivery()).isEqualTo(MailDelivery.SENT);
		assertThat(response.to()).isEqualTo("koc@example.com");
		assertThat(response.detail()).isNull();
	}

	@Test
	void shouldPassTheServersReasonOnWhenItFails() {
		when(mailSender.deliver(any(), any(), any()))
				.thenReturn(MailResult.failed("535 Username and Password not accepted"));

		var response = service.sendTo(coach());

		assertThat(response.delivery()).isEqualTo(MailDelivery.FAILED);
		assertThat(response.detail()).isEqualTo("535 Username and Password not accepted");
	}

	@Test
	void shouldSayWhenNoMailServerIsConfiguredAtAll() {
		when(mailSender.deliver(any(), any(), any())).thenReturn(MailResult.notConfigured());

		assertThat(service.sendTo(coach()).delivery()).isEqualTo(MailDelivery.NOT_CONFIGURED);
	}
}
