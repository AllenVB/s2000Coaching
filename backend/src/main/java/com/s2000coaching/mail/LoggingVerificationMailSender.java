package com.s2000coaching.mail;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.VerificationPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development fallback: logs the one-time code instead of mailing it. Active only when no
 * SMTP host is configured, so nothing silently swallows production mail.
 */
public class LoggingVerificationMailSender implements VerificationMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingVerificationMailSender.class);

	@Override
	public MailDelivery sendVerificationCode(Member member, String code, VerificationPurpose purpose) {
		log.warn("SMTP yapılandırılmadı (spring.mail.host boş). {} kodu e-posta ile GÖNDERİLMEDİ — "
				+ "alıcı: {}, amaç: {}, kod: {}", purpose, member.getEmail(), purpose, code);
		return MailDelivery.NOT_CONFIGURED;
	}
}
