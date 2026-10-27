package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.payment.IyzicoClient;
import com.s2000coaching.payment.PaymentFailedException;
import com.s2000coaching.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IyzicoCallbackServiceTest {

	private static final String TOKEN = "tok-123";

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private IyzicoClient client;
	@Mock
	private MembershipService membershipService;

	private IyzicoCallbackService service;
	private Order order;

	@BeforeEach
	void setUp() {
		service = new IyzicoCallbackService(orderRepository, client, membershipService,
				Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC));
		var pack = new CoachingPackage(UUID.randomUUID(), "kocluk-3-ay", PackageCategory.KOCLUK, "3 Aylık Koçluk",
				null, 3, 400000L, 450000L, null, false, 1, true);
		order = new Order(UUID.randomUUID(), "S2K-ABCD1234", pack, "Ayşe Yılmaz", "ayse@example.com", "+905551112233",
				null, 400000L, OrderStatus.AWAITING_PAYMENT, true, Instant.now());
		order.setPaymentReference(TOKEN);
	}

	private void orderWaiting() {
		when(orderRepository.findByPaymentReference(TOKEN)).thenReturn(Optional.of(order));
	}

	private static IyzicoClient.Result paid(String basket, String amount) {
		return new IyzicoClient.Result(true, basket, new BigDecimal(amount), null);
	}

	@Test
	void shouldMarkTheOrderPaidAndGrantTheMembershipWhenEverythingMatches() {
		orderWaiting();
		when(client.retrieve(TOKEN)).thenReturn(paid("S2K-ABCD1234", "4000.00"));

		var result = service.complete(TOKEN);

		assertThat(result.outcome()).isEqualTo(IyzicoCallbackService.Outcome.PAID);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getPaidAt()).isEqualTo(Instant.parse("2026-10-27T09:00:00Z"));
		verify(membershipService).grantFor(order);
	}

	/** The amount the provider charged must be what the order costs; anything else is not "paid". */
	@Test
	void shouldNotCallItPaidWhenTheAmountDiffers() {
		orderWaiting();
		when(client.retrieve(TOKEN)).thenReturn(paid("S2K-ABCD1234", "1.00"));

		var result = service.complete(TOKEN);

		assertThat(result.outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
		verify(membershipService, never()).grantFor(any());
	}

	/** A token issued for another order must not settle this one. */
	@Test
	void shouldNotCallItPaidWhenThePaymentWasOpenedForAnotherOrder() {
		orderWaiting();
		when(client.retrieve(TOKEN)).thenReturn(paid("S2K-OTHER999", "4000.00"));

		assertThat(service.complete(TOKEN).outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
	}

	@Test
	void shouldMarkTheOrderFailedWhenTheProviderRefuses() {
		orderWaiting();
		when(client.retrieve(TOKEN)).thenReturn(new IyzicoClient.Result(false, "S2K-ABCD1234", null, "Kart reddedildi"));

		var result = service.complete(TOKEN);

		assertThat(result.outcome()).isEqualTo(IyzicoCallbackService.Outcome.FAILED);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
		verify(membershipService, never()).grantFor(any());
	}

	@Test
	void shouldLeaveTheOrderWaitingWhenTheProviderCannotBeAsked() {
		orderWaiting();
		when(client.retrieve(TOKEN)).thenThrow(new PaymentFailedException("zaman aşımı"));

		var result = service.complete(TOKEN);

		assertThat(result.outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
	}

	/** Refresh or back button: the second visit must not pay (or fail) anything twice. */
	@Test
	void shouldDoNothingTheSecondTimeForAnAlreadyPaidOrder() {
		order.markPaid(Instant.now());
		orderWaiting();

		var result = service.complete(TOKEN);

		assertThat(result.outcome()).isEqualTo(IyzicoCallbackService.Outcome.PAID);
		verify(client, never()).retrieve(anyString());
		verify(membershipService, never()).grantFor(any());
	}

	@Test
	void shouldNotTrustAnUnknownOrMissingToken() {
		when(orderRepository.findByPaymentReference("nope")).thenReturn(Optional.empty());

		assertThat(service.complete("nope").outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		assertThat(service.complete(null).outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		assertThat(service.complete("  ").outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		verify(client, never()).retrieve(anyString());
	}

	/** Only a card order that is actually waiting can be settled by a callback. */
	@Test
	void shouldNotSettleAnOrderThatIsNotAwaitingAPayment() {
		order.setStatus(OrderStatus.AWAITING_TRANSFER);
		orderWaiting();

		assertThat(service.complete(TOKEN).outcome()).isEqualTo(IyzicoCallbackService.Outcome.UNKNOWN);
		verify(client, never()).retrieve(anyString());
		assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_TRANSFER);
	}
}
