package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.s2000coaching.service.IyzicoCallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(IyzicoCallbackController.class)
@TestPropertySource(properties = "app.public-url=https://s2000.example/")
class IyzicoCallbackControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private IyzicoCallbackService callbackService;

	private String redirectFor(String token, IyzicoCallbackService.Result result) {
		when(callbackService.complete(token)).thenReturn(result);
		var response = mvc.post().uri("/api/payments/iyzico/callback")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("token", token)
				.exchange();
		assertThat(response).hasStatus(HttpStatus.SEE_OTHER);
		assertThat(response.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
		return response.getResponse().getHeader("Location");
	}

	@Test
	void shouldSendAPaidBuyerBackWithTheOrderNumber() {
		assertThat(redirectFor("t1",
				new IyzicoCallbackService.Result(IyzicoCallbackService.Outcome.PAID, "S2K-ABCD1234")))
				.isEqualTo("https://s2000.example/?odeme=basarili&siparis=S2K-ABCD1234");
	}

	@Test
	void shouldReportAFailedPayment() {
		assertThat(redirectFor("t2",
				new IyzicoCallbackService.Result(IyzicoCallbackService.Outcome.FAILED, "S2K-ABCD1234")))
				.contains("odeme=basarisiz");
	}

	/** An unknown token gives an "unclear" answer and no order number to probe. */
	@Test
	void shouldNotRevealAnythingForAnUnknownToken() {
		assertThat(redirectFor("zzz", new IyzicoCallbackService.Result(IyzicoCallbackService.Outcome.UNKNOWN, null)))
				.isEqualTo("https://s2000.example/?odeme=belirsiz");
	}
}
