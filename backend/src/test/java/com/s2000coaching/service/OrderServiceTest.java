package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.payment.PaymentGateway;
import com.s2000coaching.payment.PaymentResult;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.web.dto.CreateOrderRequest;
import com.s2000coaching.web.dto.OrderResponse;
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
	private PaymentGateway paymentGateway;

	private OrderService orderService;

	private CoachingPackage sixMonthPackage;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(orderRepository, packageService, paymentGateway);
		sixMonthPackage = new CoachingPackage(UUID.randomUUID(), "kocluk-6-ay", PackageCategory.KOCLUK,
				"6 Aylık Koçluk", "tagline", 6, 690000L, 890000L, "EN ÇOK TERCİH EDİLEN", true, 2, true);
	}

	@Nested
	class CreateOrder {

		@Test
		void shouldCreateOrderWithServerSidePriceWhenPaymentSucceeds() {
			var request = new CreateOrderRequest("Ayşe Yılmaz", "ayse@example.com", "+905551112233", "34, İstanbul",
					sixMonthPackage.getId(), true);
			when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);
			when(paymentGateway.charge(any())).thenReturn(new PaymentResult(true, "MOCK-ref"));
			when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			OrderResponse response = orderService.createOrder(request);

			assertThat(response.status()).isEqualTo(OrderStatus.MOCK_PAID);
			assertThat(response.priceMinor()).isEqualTo(sixMonthPackage.getPriceMinor());
			assertThat(response.orderNumber()).startsWith("S2K-");

			ArgumentCaptor<com.s2000coaching.domain.Order> captor = ArgumentCaptor
					.forClass(com.s2000coaching.domain.Order.class);
			verify(orderRepository).save(captor.capture());
			assertThat(captor.getValue().getPriceMinor()).isEqualTo(690000L);
		}

		@Test
		void shouldMarkOrderFailedWhenPaymentGatewayDeclines() {
			var request = new CreateOrderRequest("Ayşe Yılmaz", "ayse@example.com", "+905551112233", null,
					sixMonthPackage.getId(), true);
			when(packageService.getActiveOrThrow(sixMonthPackage.getId())).thenReturn(sixMonthPackage);
			when(paymentGateway.charge(any())).thenReturn(new PaymentResult(false, null));
			when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			OrderResponse response = orderService.createOrder(request);

			assertThat(response.status()).isEqualTo(OrderStatus.FAILED);
		}
	}
}
