package com.s2000coaching.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A small in-memory "at most N per hour per key" limiter for public endpoints without a login. */
public final class HourlyLimiter {

	private final int maxPerHour;
	private final Map<String, Deque<Instant>> recent = new ConcurrentHashMap<>();

	public HourlyLimiter(int maxPerHour) {
		this.maxPerHour = maxPerHour;
	}

	/** True and recorded when {@code key} is still under its limit at {@code now}; false when it is not. */
	public boolean tryAcquire(String key, Instant now) {
		Deque<Instant> times = recent.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (times) {
			while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(Duration.ofHours(1)))) {
				times.pollFirst();
			}
			if (times.size() >= maxPerHour) {
				return false;
			}
			times.addLast(now);
			return true;
		}
	}
}
