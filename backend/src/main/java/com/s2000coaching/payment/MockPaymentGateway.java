package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Always-succeeds payment gateway used until a real provider is integrated.
 * No card data is collected anywhere in this flow.
 */
@Service
public class MockPaymentGateway implements PaymentGateway {

	@Override
	public PaymentResult charge(Order order) {
		return new PaymentResult(true, "MOCK-" + UUID.randomUUID());
	}
}
