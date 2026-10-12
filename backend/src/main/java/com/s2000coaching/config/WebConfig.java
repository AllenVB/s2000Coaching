package com.s2000coaching.config;

import com.s2000coaching.service.CoachAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;
	private final CoachAuthService coachAuthService;

	public WebConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins,
			@Lazy CoachAuthService coachAuthService) {
		this.allowedOrigins = allowedOrigins.split(",");
		this.coachAuthService = coachAuthService;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new CoachAuthInterceptor(coachAuthService))
				.addPathPatterns("/api/admin/**")
				// Signing in cannot itself require a session.
				.excludePathPatterns("/api/admin/auth/login");
	}
}
