package com.s2000coaching.mail;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Picks the mail sender implementation. Bean methods are evaluated top-down, so the SMTP
 * sender wins whenever {@code spring.mail.host} is set and the logging fallback is only
 * registered when it is not.
 */
@Configuration
public class MailConfig {

	/**
	 * Not {@code @ConditionalOnProperty}: application.yml declares {@code spring.mail.host} as
	 * {@code ${MAIL_HOST:}}, so with MAIL_HOST unset the property is present but empty, and
	 * that annotation counts any value other than "false" as set. The SMTP sender would then be
	 * chosen with no host, every code would fail to send, and — because the logging fallback
	 * is never registered — it would not be logged either, leaving a developer with no way to
	 * read the code. A blank host has to mean "not configured".
	 */
	@Bean
	@ConditionalOnExpression("!'${spring.mail.host:}'.isBlank()")
	VerificationMailSender smtpVerificationMailSender(
			JavaMailSender mailSender,
			@Value("${app.mail.from}") String from,
			@Value("${app.verification.code-ttl}") Duration codeTtl) {
		return new SmtpVerificationMailSender(mailSender, from, codeTtl);
	}

	@Bean
	@ConditionalOnExpression("!'${spring.mail.host:}'.isBlank()")
	NotificationMailSender smtpNotificationMailSender(JavaMailSender mailSender,
			@Value("${app.mail.from}") String from) {
		return new SmtpNotificationMailSender(mailSender, from);
	}

	@Bean
	@ConditionalOnMissingBean(NotificationMailSender.class)
	NotificationMailSender loggingNotificationMailSender() {
		return new LoggingNotificationMailSender();
	}

	@Bean
	@ConditionalOnMissingBean(VerificationMailSender.class)
	VerificationMailSender loggingVerificationMailSender() {
		return new LoggingVerificationMailSender();
	}
}
