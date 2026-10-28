package com.s2000coaching.config;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.core.env.Environment;

/**
 * The one time zone the business runs on. "Today" decides which day a workout or measurement
 * belongs to, whether a membership has ended and when the morning e-mails go out, so it must not
 * depend on where the server happens to sit: a cloud host is usually on UTC, which is three hours
 * behind Turkey and puts everything between midnight and 03:00 on the wrong day.
 */
public final class AppTimeZone {

	public static final String PROPERTY = "app.time-zone";
	public static final String DEFAULT = "Europe/Istanbul";

	private AppTimeZone() {
	}

	/** @throws java.time.DateTimeException when the configured name is not a real zone */
	public static ZoneId resolve(Environment environment) {
		return ZoneId.of(environment.getProperty(PROPERTY, DEFAULT).trim());
	}

	/**
	 * Makes the JVM agree with the configured zone. Bean-validation's {@code @PastOrPresent} and
	 * any plain {@code LocalDate.now()} read the JVM default, so leaving it on the host's zone
	 * would have the server reject today's date as "in the future" at 00:30 Istanbul time.
	 */
	public static void applyToJvm(Environment environment) {
		TimeZone.setDefault(TimeZone.getTimeZone(resolve(environment)));
	}
}
