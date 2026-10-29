package com.s2000coaching.mail;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpNotificationMailSender implements NotificationMailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpNotificationMailSender.class);

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpNotificationMailSender(JavaMailSender mailSender, String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public boolean delivers() {
		return true;
	}

	@Override
	public MailResult deliver(String to, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		try {
			mailSender.send(message);
			return MailResult.sent();
		}
		catch (MailException ex) {
			// Not rethrown: one bad address must not stop the rest of the daily run, and the
			// caller leaves the reminder unlogged so tomorrow's run tries again.
			log.error("E-posta {} adresine gönderilemedi: {}", to, subject, ex);
			return MailResult.failed(reasonOf(ex));
		}
	}

	/**
	 * The mail server's own words. The innermost jakarta.mail message is the useful one
	 * ("Could not connect to SMTP host: smtp.example.com, port: 587", "535 Authentication
	 * credentials invalid"); the exception beneath it is often just a host name, and the Spring
	 * wrapper above it only says "Mail server connection failed".
	 */
	static String reasonOf(Throwable failure) {
		Throwable mailError = null;
		Throwable root = failure;
		for (Throwable cause = failure; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
			root = cause;
			if (cause instanceof MessagingException) {
				mailError = cause;
			}
		}
		Throwable chosen = mailError != null ? mailError : root;
		String message = chosen.getMessage() != null ? chosen.getMessage() : chosen.getClass().getSimpleName();
		message = message.replaceAll("\\s+", " ").trim();
		return message.length() > 300 ? message.substring(0, 300) + "…" : message;
	}
}
