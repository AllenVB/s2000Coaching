package com.s2000coaching.mail;

/**
 * Seam for service e-mails that are not one-time codes: reminders to members and the coach's
 * daily summary. Uses the same SMTP settings as {@link VerificationMailSender}.
 *
 * <p>{@link #delivers()} is false for the development fallback, which only logs. Jobs that
 * record "sent" check it first, so nothing is marked sent that never left the building — and
 * enabling SMTP later still sends everything that is due.
 */
public interface NotificationMailSender {

	/** Whether {@link #send} really mails, as opposed to only logging. */
	boolean delivers();

	/** @return true if the message was handed to the mail server */
	boolean send(String to, String subject, String body);
}
