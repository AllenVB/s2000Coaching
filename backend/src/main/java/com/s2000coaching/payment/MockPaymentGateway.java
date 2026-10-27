package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PaymentMethod;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Development stand-in: settles every order at once and collects no card data. It is only
 * offered when no real method is configured (see {@link PaymentMethods}), so a deployment with
 * an IBAN set can never mark an order paid without money arriving.
 */
@Service
public class MockPaymentGateway implements PaymentGateway {

	@Override
	public PaymentMethod method() {
		return PaymentMethod.MOCK;
	}

	@Override
	public boolean enabled() {
		return true;
	}

	@Override
	public PaymentInitiation initiate(Order order, PaymentContext context) {
		return new PaymentInitiation(OrderStatus.MOCK_PAID, "MOCK-" + UUID.randomUUID(), null);
	}
}
