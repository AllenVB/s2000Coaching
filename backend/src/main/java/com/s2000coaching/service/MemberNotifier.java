package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.mail.NotificationMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tells a member the coach has answered. The e-mail carries no part of the answer — it is health and
 * coaching content — only that there is one and where to read it. It goes out only after the answer is
 * saved, never fails the request that saved it, and respects the member's switch for reminder e-mails.
 */
@Component
public class MemberNotifier {

	private static final Logger log = LoggerFactory.getLogger(MemberNotifier.class);

	private final NotificationMailSender mailSender;
	private final String siteUrl;

	public MemberNotifier(NotificationMailSender mailSender, @Value("${app.public-url}") String siteUrl) {
		this.mailSender = mailSender;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
	}

	/**
	 * @param what what was answered, in the member's words ("mesajınıza", "form videonuza", …)
	 * @param path where to read it, e.g. {@code /uye/mesajlar}
	 */
	public void coachReplied(Member member, String what, String path) {
		if (!member.isRemindersEnabled() || !mailSender.delivers()) {
			return;
		}
		String to = member.getEmail();
		String firstName = member.getFullName().trim().split("\\s+")[0];
		AfterCommit.run(() -> {
			try {
				mailSender.send(to, "Koçunuz yanıt verdi",
						"Merhaba " + firstName + ",\n\nKoçunuz " + what + " yanıt verdi. Okumak için: " + siteUrl + path
								+ "\n\nBu e-postaları Profilim sayfasından kapatabilirsiniz.\nS2000 Coaching\n" + siteUrl);
			} catch (RuntimeException e) {
				log.warn("Yanıt bildirimi gönderilemedi", e);
			}
		});
	}
}
