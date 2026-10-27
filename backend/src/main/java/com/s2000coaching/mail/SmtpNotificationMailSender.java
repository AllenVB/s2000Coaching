package com.s2000coaching.mail;

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
	public boolean send(String to, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		try {
			mailSender.send(message);
			return true;
		}
		catch (MailException ex) {
			// Not rethrown: one bad address must not stop the rest of the daily run, and the
			// caller leaves the reminder unlogged so tomorrow's run tries again.
			log.error("E-posta {} adresine gönderilemedi: {}", to, subject, ex);
			return false;
		}
	}
}
