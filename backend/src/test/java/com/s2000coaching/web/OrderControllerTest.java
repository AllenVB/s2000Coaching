package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.service.OrderService;
import com.s2000coaching.web.dto.CreateOrderRequest;
import com.s2000coaching.web.dto.OrderResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private OrderService orderService;

	@Test
	void shouldReturn201WithCreatedOrderWhenRequestIsValid() {
		UUID packageId = UUID.randomUUID();
		var request = new CreateOrderRequest("Ayşe Yılmaz", "ayse@example.com", "+905551112233", "34, İstanbul",
				packageId, true);
		var response = new OrderResponse("S2K-ABCD1234", "6 Aylık Koçluk", 690000L, OrderStatus.MOCK_PAID,
				Instant.parse("2026-09-15T10:00:00Z"));
		when(orderService.createOrder(any())).thenReturn(response);

		assertThat(mvc.post().uri("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.orderNumber").isEqualTo("S2K-ABCD1234");
	}

	@Test
	void shouldReturn400WhenKvkkConsentIsMissing() {
		String invalidJson = """
				{
				  "customerName": "Ayşe Yılmaz",
				  "email": "ayse@example.com",
				  "phone": "+905551112233",
				  "packageId": "%s",
				  "kvkkConsent": false
				}
				""".formatted(UUID.randomUUID());

		assertThat(mvc.post().uri("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}
}
