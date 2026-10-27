package com.s2000coaching.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One clock for anything that reasons about "today", so tests can pin the date. */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}
}
