package com.s2000coaching.payment;

import com.s2000coaching.domain.OrderStatus;

/**
 * What starting a payment produced.
 *
 * @param status      the order's state afterwards: awaiting a transfer, awaiting the card page,
 *                    failed, or (development only) settled
 * @param reference   the provider's token or the transfer reference
 * @param redirectUrl where to send the buyer to pay, or null when nothing more is needed from them
 */
public record PaymentInitiation(OrderStatus status, String reference, String redirectUrl) {
}
