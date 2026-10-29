package com.s2000coaching.mail;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.VerificationPurpose;

/**
 * Seam for delivering one-time codes to members. {@code SmtpVerificationMailSender} is used
 * whenever {@code spring.mail.host} is configured; otherwise {@code LoggingVerificationMailSender}
 * writes the code to the application log so the flow stays usable in local development
 * (see {@link MailConfig}).
 */
public interface VerificationMailSender {

	/**
	 * @return what happened, so the coach can be told when a code never left — the member is
	 *         never told, because the answer would reveal whether an address is registered
	 */
	MailDelivery sendVerificationCode(Member member, String code, VerificationPurpose purpose);
}
