package com.s2000coaching.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Development fallback when no SMTP host is configured: reports that nothing was mailed. */
public class LoggingNotificationMailSender implements NotificationMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingNotificationMailSender.class);

	@Override
	public boolean delivers() {
		return false;
	}

	@Override
	public MailResult deliver(String to, String subject, String body) {
		log.info("SMTP yapılandırılmadı; e-posta gönderilmedi — alıcı: {}, konu: {}", to, subject);
		return MailResult.notConfigured();
	}
}
