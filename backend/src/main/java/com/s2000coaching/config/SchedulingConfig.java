package com.s2000coaching.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled}, which today only drives the nightly cleanup of expired
 * sessions and verification codes.
 *
 * <p>On more than one instance every one of them would run the cleanup; that is harmless
 * here because the deletes are bounded by a timestamp and two runs delete the same rows,
 * but a job with side effects would need a lock.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
