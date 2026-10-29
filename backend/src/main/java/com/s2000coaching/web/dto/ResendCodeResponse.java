package com.s2000coaching.web.dto;

import com.s2000coaching.mail.MailDelivery;

public record ResendCodeResponse(MailDelivery delivery) {
}
