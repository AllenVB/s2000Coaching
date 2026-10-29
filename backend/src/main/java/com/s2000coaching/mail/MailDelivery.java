package com.s2000coaching.mail;

/**
 * What happened to one e-mail, in terms the coach can act on. {@code SENT} means the mail
 * server accepted it — not that it reached an inbox, which no sender can promise.
 */
public enum MailDelivery {

	/** Handed to the mail server. */
	SENT,

	/** No SMTP host is set (development): the message was only written to the server log. */
	NOT_CONFIGURED,

	/** The mail server refused or could not be reached. The reason is in the server log. */
	FAILED
}
