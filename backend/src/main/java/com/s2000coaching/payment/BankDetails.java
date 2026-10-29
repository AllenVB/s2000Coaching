package com.s2000coaching.payment;

/** Where to send a bank transfer. {@code reference} is what the buyer must put in the description. */
public record BankDetails(String iban, String accountHolder, String bankName, String reference) {
}
