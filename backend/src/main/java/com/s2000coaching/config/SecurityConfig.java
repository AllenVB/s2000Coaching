package com.s2000coaching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Only the password encoder. The full Spring Security starter is deliberately not used: this
 * app authenticates members and coaches with the same opaque-bearer-token interceptor, and a
 * servlet filter chain would duplicate that rather than add to it.
 */
@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
