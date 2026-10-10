package com.s2000coaching.config;

import com.s2000coaching.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Shared-secret gate for {@code /api/admin/**}. The management page sends the key as
 * {@code X-Admin-Token}; it is compared in constant time against {@code app.admin.token}.
 *
 * <p>This is deliberately the smallest thing that keeps the member administration endpoints
 * off the public internet — it is not user accounts. Replace it with real authentication
 * (e.g. Spring Security) before more than the coach needs access.
 */
public class AdminTokenInterceptor implements HandlerInterceptor {

	static final String HEADER_NAME = "X-Admin-Token";

	private final String expectedToken;

	public AdminTokenInterceptor(String expectedToken) {
		this.expectedToken = expectedToken;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// CORS preflight carries no custom headers; the browser sends the real request next.
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}

		if (expectedToken.isBlank()) {
			throw new UnauthorizedException(
					"Yönetim erişimi yapılandırılmamış: app.admin.token (ADMIN_TOKEN) tanımlanmalı.");
		}

		String provided = request.getHeader(HEADER_NAME);
		if (provided == null || !MessageDigest.isEqual(
				expectedToken.getBytes(StandardCharsets.UTF_8),
				provided.getBytes(StandardCharsets.UTF_8))) {
			throw new UnauthorizedException("Yönetim anahtarı geçersiz.");
		}
		return true;
	}
}
