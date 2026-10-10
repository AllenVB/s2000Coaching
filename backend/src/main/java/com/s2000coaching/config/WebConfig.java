package com.s2000coaching.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;
	private final String adminToken;

	public WebConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins,
			@Value("${app.admin.token}") String adminToken) {
		this.allowedOrigins = allowedOrigins.split(",");
		this.adminToken = adminToken;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminTokenInterceptor(adminToken))
				.addPathPatterns("/api/admin/**");
	}
}
