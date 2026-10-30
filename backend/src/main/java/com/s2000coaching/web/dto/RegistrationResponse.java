package com.s2000coaching.web.dto;

/**
 * What sign-up leads to. When the e-mail has to be confirmed first, {@code session} is null and
 * the page asks for the mailed code; otherwise the member is signed in straight away.
 */
public record RegistrationResponse(boolean verificationRequired, MemberSessionResponse session) {
}
