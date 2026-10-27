package com.s2000coaching.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

class MailConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(MailConfig.class)
			.withInitializer(context -> context.getBeanFactory()
					.setConversionService(ApplicationConversionService.getSharedInstance()))
			.withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
			.withPropertyValues("app.mail.from=no-reply@example.com", "app.verification.code-ttl=15m");

	/** The shape application.yml produces when MAIL_HOST is not set: present but empty. */
	@Test
	void shouldLogCodesWhenTheHostIsEmpty() {
		runner.withPropertyValues("spring.mail.host=").run(context ->
				assertThat(context.getBean(VerificationMailSender.class))
						.isInstanceOf(LoggingVerificationMailSender.class));
	}

	@Test
	void shouldLogCodesWhenTheHostIsAbsent() {
		runner.run(context -> assertThat(context.getBean(VerificationMailSender.class))
				.isInstanceOf(LoggingVerificationMailSender.class));
	}

	@Test
	void shouldUseSmtpWhenAHostIsConfigured() {
		runner.withPropertyValues("spring.mail.host=smtp.example.com").run(context ->
				assertThat(context.getBean(VerificationMailSender.class))
						.isInstanceOf(SmtpVerificationMailSender.class));
	}
}
