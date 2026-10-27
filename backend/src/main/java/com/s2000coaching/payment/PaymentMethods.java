package com.s2000coaching.payment;

import com.s2000coaching.domain.PaymentMethod;
import com.s2000coaching.exception.BadRequestException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Which payment methods a buyer may use right now: every configured real one, or — only when
 * none is configured — the development mock. That fallback is deliberate: a fresh checkout
 * runs end to end with no setup, and a configured site cannot be tricked into the mock.
 */
@Service
public class PaymentMethods {

	private final List<PaymentGateway> gateways;

	public PaymentMethods(List<PaymentGateway> gateways) {
		this.gateways = gateways;
	}

	public List<PaymentGateway> available() {
		List<PaymentGateway> real = gateways.stream()
				.filter(gateway -> gateway.method() != PaymentMethod.MOCK && gateway.enabled())
				.toList();
		if (!real.isEmpty()) {
			return real;
		}
		return gateways.stream().filter(gateway -> gateway.method() == PaymentMethod.MOCK).toList();
	}

	/** The requested method, or the first available one when the buyer did not choose. */
	public PaymentGateway resolve(PaymentMethod requested) {
		List<PaymentGateway> available = available();
		if (requested == null) {
			return available.get(0);
		}
		return available.stream()
				.filter(gateway -> gateway.method() == requested)
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Bu ödeme yöntemi şu an kullanılamıyor."));
	}

	public BankDetails bankDetails(String reference) {
		return gateways.stream()
				.filter(BankTransferPaymentGateway.class::isInstance)
				.map(BankTransferPaymentGateway.class::cast)
				.findFirst()
				.map(gateway -> gateway.detailsFor(reference))
				.orElse(null);
	}
}
