package com.s2000coaching.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.VerificationPurpose;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** What the coach is told when the mail server does not take a message. */
class SmtpMailSendersTest {

	private JavaMailSender javaMailSender;
	private Member member;

	@BeforeEach
	void setUp() {
		javaMailSender = mock(JavaMailSender.class);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
	}

	@Test
	void shouldReportSentWhenTheServerTakesTheCode() {
		var sender = new SmtpVerificationMailSender(javaMailSender, "no-reply@example.com", Duration.ofMinutes(15));

		assertThat(sender.sendVerificationCode(member, "123456", VerificationPurpose.REGISTRATION))
				.isEqualTo(MailDelivery.SENT);
		verify(javaMailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	void shouldReportFailedInsteadOfThrowingWhenTheServerRefusesTheCode() {
		doThrow(new MailAuthenticationException("Authentication failed")).when(javaMailSender)
				.send(any(SimpleMailMessage.class));
		var sender = new SmtpVerificationMailSender(javaMailSender, "no-reply@example.com", Duration.ofMinutes(15));

		assertThat(sender.sendVerificationCode(member, "123456", VerificationPurpose.REGISTRATION))
				.isEqualTo(MailDelivery.FAILED);
	}

	@Test
	void shouldSayNotConfiguredWhenOnlyLoggingTheCode() {
		assertThat(new LoggingVerificationMailSender()
				.sendVerificationCode(member, "123456", VerificationPurpose.REGISTRATION))
				.isEqualTo(MailDelivery.NOT_CONFIGURED);
	}

	@Test
	void shouldGiveTheCoachTheServersOwnReasonForAWrongPassword() {
		doThrow(new MailAuthenticationException("Authentication failed",
				new AuthenticationFailedException("535 5.7.8 Username and Password not accepted")))
				.when(javaMailSender).send(any(SimpleMailMessage.class));
		var sender = new SmtpNotificationMailSender(javaMailSender, "no-reply@example.com");

		MailResult result = sender.deliver("koc@example.com", "Test", "gövde");

		assertThat(result.delivery()).isEqualTo(MailDelivery.FAILED);
		assertThat(result.detail()).isEqualTo("535 5.7.8 Username and Password not accepted");
	}

	/** Beneath the useful message there is often only a bare host name; the useful one is kept. */
	@Test
	void shouldKeepTheConnectionMessageRatherThanTheBareHostNameBeneathIt() {
		var cannotConnect = new MessagingException("Could not connect to SMTP host: smtp.invalid, port: 587",
				new UnknownHostException("smtp.invalid"));
		doThrow(new MailSendException("Mail server connection failed", cannotConnect))
				.when(javaMailSender).send(any(SimpleMailMessage.class));
		var sender = new SmtpNotificationMailSender(javaMailSender, "no-reply@example.com");

		assertThat(sender.deliver("koc@example.com", "Test", "gövde").detail())
				.isEqualTo("Could not connect to SMTP host: smtp.invalid, port: 587");
	}

	@Test
	void shouldFallBackToTheDeepestMessageWhenThereIsNoMailException() {
		assertThat(SmtpNotificationMailSender.reasonOf(
				new RuntimeException("outer", new IllegalStateException("inner reason"))))
				.isEqualTo("inner reason");
	}

	@Test
	void shouldNotLetAHugeServerReplyFloodTheScreen() {
		String reason = SmtpNotificationMailSender.reasonOf(new MessagingException("x".repeat(1000)));

		assertThat(reason).hasSize(301).endsWith("…");
	}

	@Test
	void shouldStillAnswerTrueOrFalseForTheDailyJobs() {
		var sender = new SmtpNotificationMailSender(javaMailSender, "no-reply@example.com");
		assertThat(sender.send("uye@example.com", "Konu", "gövde")).isTrue();

		doThrow(new MailSendException("down")).when(javaMailSender).send(any(SimpleMailMessage.class));
		assertThat(sender.send("uye@example.com", "Konu", "gövde")).isFalse();
	}

	@Test
	void shouldNeverClaimToDeliverWhenOnlyLogging() {
		var sender = new LoggingNotificationMailSender();

		assertThat(sender.delivers()).isFalse();
		assertThat(sender.send("uye@example.com", "Konu", "gövde")).isFalse();
		assertThat(sender.deliver("uye@example.com", "Konu", "gövde").delivery())
				.isEqualTo(MailDelivery.NOT_CONFIGURED);
	}
}
