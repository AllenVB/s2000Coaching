package com.s2000coaching.mail;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

	@Bean
	@ConditionalOnProperty(name = "spring.mail.host")
	VerificationMailSender smtpVerificationMailSender(
			JavaMailSender mailSender,
			@Value("${app.mail.from}") String from,
			@Value("${app.verification.code-ttl}") Duration codeTtl) {
		return new SmtpVerificationMailSender(mailSender, from, codeTtl);
	}

	@Bean
	@ConditionalOnMissingBean(VerificationMailSender.class)
	VerificationMailSender loggingVerificationMailSender() {
		return new LoggingVerificationMailSender();
	}
}
