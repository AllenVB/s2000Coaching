package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * IBAN / havale. Nothing is charged: the order waits as {@code AWAITING_TRANSFER} until the
 * coach sees the money and confirms it, and only that confirmation grants a membership.
 *
 * <p>The order number is the transfer reference, so each incoming payment can be matched to an
 * order by reading the bank statement description.
 */
@Service
public class BankTransferPaymentGateway implements PaymentGateway {

	private final String iban;
	private final String accountHolder;
	private final String bankName;

	public BankTransferPaymentGateway(@Value("${app.payment.iban.number:}") String iban,
			@Value("${app.payment.iban.account-holder:}") String accountHolder,
			@Value("${app.payment.iban.bank-name:}") String bankName) {
		this.iban = iban.replaceAll("\\s+", "");
		this.accountHolder = accountHolder.trim();
		this.bankName = bankName.trim();
	}

	@Override
	public PaymentMethod method() {
		return PaymentMethod.IBAN;
	}

	@Override
	public boolean enabled() {
		return !iban.isBlank() && !accountHolder.isBlank();
	}

	@Override
	public PaymentInitiation initiate(Order order) {
		return new PaymentInitiation(OrderStatus.AWAITING_TRANSFER, order.getOrderNumber(), null);
	}

	public BankDetails detailsFor(String reference) {
		return new BankDetails(iban, accountHolder, bankName, reference);
	}
}
