package com.s2000coaching.config;

import com.s2000coaching.service.CoachAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Gate for {@code /api/admin/**}: every route needs a coach session, obtained from
 * {@code /api/admin/auth/login}. Replaces the shared-secret header.
 */
public class CoachAuthInterceptor implements HandlerInterceptor {

	private final CoachAuthService coachAuthService;

	public CoachAuthInterceptor(CoachAuthService coachAuthService) {
		this.coachAuthService = coachAuthService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// CORS preflight carries no Authorization header; the browser sends the real request next.
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}
		coachAuthService.requireCoach(request.getHeader(HttpHeaders.AUTHORIZATION));
		return true;
	}
}
