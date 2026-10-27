package com.s2000coaching.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Order;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.domain.PaymentMethod;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IyzicoPaymentGatewayTest {

	@Mock
	private IyzicoClient client;

	private IyzicoPaymentGateway gateway;
	private Order order;

	@BeforeEach
	void setUp() {
		gateway = new IyzicoPaymentGateway(client);
		var pack = new CoachingPackage(UUID.randomUUID(), "kocluk-3-ay", PackageCategory.KOCLUK, "3 Aylık Koçluk",
				null, 3, 400000L, 450000L, null, false, 1, true);
		order = new Order(UUID.randomUUID(), "S2K-ABCD1234", pack, "Ayşe Yılmaz", "ayse@example.com", "+905551112233",
				null, 400000L, OrderStatus.PENDING, true, Instant.now());
	}

	@Test
	void shouldSendTheBuyerToTheCardPageWithoutChargingAnything() {
		when(client.initialize(order, "203.0.113.9"))
				.thenReturn(new IyzicoClient.Initialized("tok-1", "https://sandbox.iyzico.test/pay/tok-1"));

		var initiation = gateway.initiate(order, new PaymentContext("203.0.113.9"));

		assertThat(initiation.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
		assertThat(initiation.reference()).isEqualTo("tok-1");
		assertThat(initiation.redirectUrl()).isEqualTo("https://sandbox.iyzico.test/pay/tok-1");
	}

	@Test
	void shouldMarkTheOrderFailedWhenTheCardPageCannotBeOpened() {
		when(client.initialize(any(), anyString())).thenThrow(new PaymentFailedException("anahtar geçersiz"));

		var initiation = gateway.initiate(order, new PaymentContext("203.0.113.9"));

		assertThat(initiation.status()).isEqualTo(OrderStatus.FAILED);
		assertThat(initiation.redirectUrl()).isNull();
	}

	@Test
	void shouldBeOfferedOnlyWhenConfigured() {
		when(client.configured()).thenReturn(false, true);

		assertThat(gateway.enabled()).isFalse();
		assertThat(gateway.enabled()).isTrue();
	}

	/** With a card provider configured the development mock disappears, as it does with an IBAN. */
	@Test
	void shouldReplaceTheMockWhenConfigured() {
		when(client.configured()).thenReturn(true);
		var methods = new PaymentMethods(List.of(new MockPaymentGateway(), gateway));

		assertThat(methods.available()).extracting(PaymentGateway::method).containsExactly(PaymentMethod.IYZICO);
	}

	@Test
	void shouldConvertKurusToLiraAndSplitTheName() {
		assertThat(IyzipaySdkClient.tl(400000L)).isEqualByComparingTo("4000.00");
		assertThat(IyzipaySdkClient.tl(129050L).toPlainString()).isEqualTo("1290.50");
		assertThat(IyzipaySdkClient.splitName("Ayşe Nur Yılmaz")).containsExactly("Ayşe Nur", "Yılmaz");
		assertThat(IyzipaySdkClient.splitName("Ahmet")).containsExactly("Ahmet", "Ahmet");
	}
}
