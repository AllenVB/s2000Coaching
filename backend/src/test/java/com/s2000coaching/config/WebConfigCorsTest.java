package com.s2000coaching.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.MemberService;
import com.s2000coaching.web.MemberController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The browser sends a preflight before any request carrying an {@code Authorization} header,
 * and refuses the real request when the method is missing from the response. That refusal
 * never reaches the server, so it is invisible to every other test here — which is how a
 * missing {@code PATCH} once shipped and only showed up as "Failed to fetch" in the UI.
 */
@WebMvcTest(MemberController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:7000")
class WebConfigCorsTest {

	private static final String ORIGIN = "http://localhost:7000";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private CoachAuthService coachAuthService;

	@MockitoBean
	private MemberAuthService memberAuthService;

	@MockitoBean
	private MemberService memberService;

	private MvcTestResult preflight(String origin, String method) {
		return mvc.options().uri("/api/member/me")
				.header(HttpHeaders.ORIGIN, origin)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
				.exchange();
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
	void shouldAllowEveryMethodTheFrontendUses(String method) {
		MvcTestResult result = preflight(ORIGIN, method);

		assertThat(result).hasStatus(HttpStatus.OK)
				.hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN);
		assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
				.contains(method);
	}

	@Test
	void shouldRejectAMethodThatIsNotAllowed() {
		// The guard itself: a method left out of the list fails here rather than in the UI.
		assertThat(preflight(ORIGIN, "TRACE")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldRejectAnUnknownOrigin() {
		assertThat(preflight("http://kotu-site.example", "GET")).hasStatus(HttpStatus.FORBIDDEN);
	}
}
