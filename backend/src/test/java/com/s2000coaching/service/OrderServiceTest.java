package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.domain.PaymentMethod;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.payment.BankTransferPaymentGateway;
import com.s2000coaching.payment.MockPaymentGateway;
import com.s2000coaching.payment.PaymentMethods;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.web.dto.CreateOrderRequest;
import com.s2000coaching.web.dto.OrderResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private PackageService packageService;

	@Mock
	private CouponService couponService;

	private CoachingPackage sixMonthPackage;

	@BeforeEach
	void setUp() {
		sixMonthPackage = new CoachingPackage(UUID.randomUUID(), "kocluk-6-ay", PackageCategory.KOCLUK,
				"6 Aylık Koçluk", "tagline", 6, 690000L, 890000L, "EN ÇOK TERCİH EDİLEN", true, 2, true);
	}

	private OrderService service(String iban) {
		var bank = new BankTransferPaymentGateway(iban, iban.isBlank() ? "" : "Süleyman Arlı", "Banka");
		return new OrderService(orderRepository, packageService,
				new PaymentMethods(List.of(new MockPaymentGateway(), bank)), couponService);
	}

	private CreateOrderRequest request(PaymentMethod method) {
		return new CreateOrderRequest("Ayşe Yılmaz", "ayse@example.com", "+905551112233", "34, İstanbul",
				sixMonthPackage.getId(), true, method);
	}

	@Test
	void shouldChargeTheDiscountedPriceWhenACodeIsGiven() {
		var coupon = new com.s2000coaching.domain.Coupon(UUID.randomUUID(), "YAZ20", 20, null, null, java.time.Instant.now());
		when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);
		when(couponService.redeem("yaz20")).thenReturn(coupon);
		when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var withCode = new CreateOrderRequest("Ayşe Yılmaz", "ayse@example.com", "+905551112233", "34, İstanbul",
				sixMonthPackage.getId(), true, null, "yaz20");
		OrderResponse response = service("").createOrder(withCode, "203.0.113.9");

		assertThat(response.priceMinor()).isEqualTo(552000L);
	}

	@Nested
	class WithNothingConfigured {

		@Test
		void shouldSettleWithTheDevelopmentGatewayAtTheServerSidePrice() {
			when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);
			when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			OrderResponse response = service("").createOrder(request(null), "203.0.113.9");

			assertThat(response.status()).isEqualTo(OrderStatus.MOCK_PAID);
			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.MOCK);
			assertThat(response.priceMinor()).isEqualTo(690000L);
			assertThat(response.orderNumber()).startsWith("S2K-");
			assertThat(response.bankTransfer()).isNull();
		}
	}

	@Nested
	class WithAnIbanConfigured {

		@Test
		void shouldWaitForTheTransferAndTellTheBuyerWhereToSendIt() {
			when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);
			when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			OrderResponse response = service("TR33 0006 1005 1978 6457 8413 26").createOrder(request(null), "203.0.113.9");

			assertThat(response.status()).isEqualTo(OrderStatus.AWAITING_TRANSFER);
			assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.IBAN);
			assertThat(response.bankTransfer().iban()).isEqualTo("TR330006100519786457841326");
			// The order number is the reference, so each transfer can be matched to an order.
			assertThat(response.bankTransfer().reference()).isEqualTo(response.orderNumber());
		}

		/** A configured site must never be talked into the mock that marks orders paid. */
		@Test
		void shouldRefuseTheMockMethodWhenARealOneIsConfigured() {
			when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);

			assertThatThrownBy(() -> service("TR330006100519786457841326").createOrder(request(PaymentMethod.MOCK), "203.0.113.9"))
					.isInstanceOf(BadRequestException.class);
			verify(orderRepository, never()).save(any());
		}

		@Test
		void shouldRecordTheMethodAndReferenceOnTheOrder() {
			when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);
			when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			service("TR330006100519786457841326").createOrder(request(PaymentMethod.IBAN), "203.0.113.9");

			ArgumentCaptor<com.s2000coaching.domain.Order> captor = ArgumentCaptor
					.forClass(com.s2000coaching.domain.Order.class);
			verify(orderRepository).save(captor.capture());
			assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.IBAN);
			assertThat(captor.getValue().getPaymentReference()).isEqualTo(captor.getValue().getOrderNumber());
		}
	}
}
