package com.s2000coaching.web;

import com.s2000coaching.domain.PaymentMethod;
import com.s2000coaching.payment.PaymentMethods;
import com.s2000coaching.web.dto.PaymentMethodResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** What the checkout offers. Public: the buyer has no account yet. */
@RestController
public class PaymentMethodController {

	private final PaymentMethods paymentMethods;

	public PaymentMethodController(PaymentMethods paymentMethods) {
		this.paymentMethods = paymentMethods;
	}

	@GetMapping("/api/payment-methods")
	public List<PaymentMethodResponse> list() {
		return paymentMethods.available().stream()
				.map(gateway -> new PaymentMethodResponse(gateway.method(), label(gateway.method()),
						// The order number is not known yet; the order response carries the full details.
						gateway.method() == PaymentMethod.IBAN ? paymentMethods.bankDetails(null) : null))
				.toList();
	}

	private static String label(PaymentMethod method) {
		return switch (method) {
			case IBAN -> "Havale / EFT (IBAN)";
			case IYZICO -> "Kredi / Banka Kartı";
			case MOCK -> "Test ödemesi (gerçek ödeme alınmaz)";
		};
	}
}
