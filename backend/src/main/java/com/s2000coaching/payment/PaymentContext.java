package com.s2000coaching.payment;

/** What a gateway may need to know about the request, beyond the order itself. */
public record PaymentContext(String clientIp) {
}
