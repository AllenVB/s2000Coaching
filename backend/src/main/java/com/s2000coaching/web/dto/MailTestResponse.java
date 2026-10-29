package com.s2000coaching.web.dto;

import com.s2000coaching.mail.MailDelivery;

/** @param detail the mail server's reason when {@code delivery} is FAILED, otherwise null */
public record MailTestResponse(MailDelivery delivery, String to, String detail) {
}
