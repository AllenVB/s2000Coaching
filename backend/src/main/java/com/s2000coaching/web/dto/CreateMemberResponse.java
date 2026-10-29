package com.s2000coaching.web.dto;

import com.s2000coaching.mail.MailDelivery;

/**
 * The member just created, and whether their registration code was mailed. The member exists
 * either way; when the mail failed the coach can resend once the mail settings are fixed.
 */
public record CreateMemberResponse(MemberResponse member, MailDelivery inviteDelivery) {
}
