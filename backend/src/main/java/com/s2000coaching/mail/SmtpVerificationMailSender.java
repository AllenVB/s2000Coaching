package com.s2000coaching.mail;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.VerificationPurpose;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Sends one-time codes over the configured SMTP server. */
public class SmtpVerificationMailSender implements VerificationMailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpVerificationMailSender.class);

	private final JavaMailSender mailSender;
	private final String from;
	private final Duration codeTtl;

	public SmtpVerificationMailSender(JavaMailSender mailSender, String from, Duration codeTtl) {
		this.mailSender = mailSender;
		this.from = from;
		this.codeTtl = codeTtl;
	}

	@Override
	public MailDelivery sendVerificationCode(Member member, String code, VerificationPurpose purpose) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(member.getEmail());
		message.setSubject(subjectFor(purpose));
		message.setText(bodyFor(member, code, purpose));

		try {
			mailSender.send(message);
			return MailDelivery.SENT;
		}
		catch (MailException ex) {
			// The code is already persisted, so a failed send is recoverable: the admin can
			// trigger a resend. Throwing would turn it into a 500 and hide the created member
			// from the UI, so the failure is returned and the coach is told.
			log.error("{} kodu {} adresine gönderilemedi", purpose, member.getEmail(), ex);
			return MailDelivery.FAILED;
		}
	}

	private String subjectFor(VerificationPurpose purpose) {
		return purpose == VerificationPurpose.REGISTRATION
				? "S2000 Coaching — Üyelik onay kodunuz"
				: "S2000 Coaching — Giriş kodunuz";
	}

	private String bodyFor(Member member, String code, VerificationPurpose purpose) {
		String intro = purpose == VerificationPurpose.REGISTRATION
				? "S2000 Coaching üyeliğiniz oluşturuldu. Üyeliğinizi tamamlamak için aşağıdaki onay kodunu kullanın:"
				: "S2000 Coaching üye alanına giriş yapmak için aşağıdaki kodu kullanın:";

		return """
				Merhaba %s,

				%s

				    %s

				Kod %d dakika boyunca geçerlidir.

				Bu isteği siz yapmadıysanız bu e-postayı yok sayabilirsiniz.

				S2000 Coaching
				""".formatted(member.getFullName(), intro, code, codeTtl.toMinutes());
	}
}
