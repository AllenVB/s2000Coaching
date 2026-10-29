package com.s2000coaching.mail;

/**
 * The outcome of {@link NotificationMailSender#deliver}. {@code detail} is the mail server's own
 * reason for a failure (wrong password, host unreachable), null otherwise; it is meant for the
 * coach's eyes only and is never sent to a member.
 */
public record MailResult(MailDelivery delivery, String detail) {

	public static MailResult sent() {
		return new MailResult(MailDelivery.SENT, null);
	}

	public static MailResult notConfigured() {
		return new MailResult(MailDelivery.NOT_CONFIGURED, null);
	}

	public static MailResult failed(String detail) {
		return new MailResult(MailDelivery.FAILED, detail);
	}
}
