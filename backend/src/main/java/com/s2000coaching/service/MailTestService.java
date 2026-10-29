package com.s2000coaching.service;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.mail.MailResult;
import com.s2000coaching.mail.NotificationMailSender;
import com.s2000coaching.web.dto.MailTestResponse;
import org.springframework.stereotype.Service;

/**
 * Sends the coach one test e-mail, so the mail settings can be checked before the first member
 * is invited rather than after a member cannot log in. It goes only to the signed-in coach's own
 * address: a button that could mail anyone would be a way to use the server to spam.
 */
@Service
public class MailTestService {

	private final NotificationMailSender mailSender;

	public MailTestService(NotificationMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public MailTestResponse sendTo(CoachAccount coach) {
		MailResult result = mailSender.deliver(coach.getEmail(), "S2000 Coaching — test e-postası",
				"""
				Merhaba,

				Bu bir test e-postasıdır. Bunu okuyabiliyorsanız e-posta ayarlarınız çalışıyor:
				üyelere onay ve giriş kodları bu ayarlarla gönderilecek.

				S2000 Coaching
				""");
		return new MailTestResponse(result.delivery(), coach.getEmail(), result.detail());
	}
}
