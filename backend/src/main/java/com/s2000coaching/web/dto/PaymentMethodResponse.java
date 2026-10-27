package com.s2000coaching.web.dto;

import com.s2000coaching.domain.PaymentMethod;
import com.s2000coaching.payment.BankDetails;

/** One method the checkout may offer; {@code bank} is only set for IBAN. */
public record PaymentMethodResponse(PaymentMethod method, String label, BankDetails bank) {
}
