package com.s2000coaching.payment;

import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PaymentMethod;
import org.springframework.stereotype.Service;

/**
 * Card payment on iyzico's hosted page. Offered only when the API keys are configured, so a
 * checkout without them never shows a card option that cannot work.
 *
 * <p>Starting a payment charges nothing: the order waits as {@code AWAITING_PAYMENT} and the
 * buyer is sent to iyzico. It becomes paid only when iyzico's callback arrives and the result,
 * fetched server to server, confirms it (see {@code IyzicoCallbackService}).
 */
@Service
public class IyzicoPaymentGateway implements PaymentGateway {

	private final IyzicoClient client;

	public IyzicoPaymentGateway(IyzicoClient client) {
		this.client = client;
	}

	@Override
	public PaymentMethod method() {
		return PaymentMethod.IYZICO;
	}

	@Override
	public boolean enabled() {
		return client.configured();
	}

	@Override
	public PaymentInitiation initiate(Order order, PaymentContext context) {
		try {
			IyzicoClient.Initialized started = client.initialize(order, context.clientIp());
			return new PaymentInitiation(OrderStatus.AWAITING_PAYMENT, started.token(), started.paymentPageUrl());
		}
		catch (PaymentFailedException e) {
			// The order exists but never reached the card page; nothing was charged.
			return new PaymentInitiation(OrderStatus.FAILED, null, null);
		}
	}
}
