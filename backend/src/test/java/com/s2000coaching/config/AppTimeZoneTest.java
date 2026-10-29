package com.s2000coaching.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AppTimeZoneTest {

	private TimeZone original;

	@BeforeEach
	void remember() {
		original = TimeZone.getDefault();
	}

	@AfterEach
	void restore() {
		TimeZone.setDefault(original);
	}

	@Test
	void shouldDefaultToIstanbulWhateverTheHostIsSetTo() {
		assertThat(AppTimeZone.resolve(new MockEnvironment())).isEqualTo(ZoneId.of("Europe/Istanbul"));
	}

	@Test
	void shouldTakeTheConfiguredZone() {
		MockEnvironment environment = new MockEnvironment().withProperty("app.time-zone", " Europe/Berlin ");

		assertThat(AppTimeZone.resolve(environment)).isEqualTo(ZoneId.of("Europe/Berlin"));
	}

	@Test
	void shouldRefuseAZoneThatDoesNotExistRatherThanFallBackSilently() {
		MockEnvironment environment = new MockEnvironment().withProperty("app.time-zone", "Mars/Olympus");

		assertThatThrownBy(() -> AppTimeZone.resolve(environment)).isInstanceOf(DateTimeException.class);
	}

	@Test
	void shouldMakeTheJvmDefaultTheSameZoneSoValidationAndPlainNowAgree() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		AppTimeZone.applyToJvm(new MockEnvironment());

		assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("Europe/Istanbul"));
	}

	@Test
	void shouldPutHalfPastMidnightIstanbulTimeOnTheNewDayEvenThoughUtcIsStillOnTheOldOne() {
		// 22:30 UTC on the 27th is 01:30 on the 28th in Istanbul (UTC+3, no daylight saving).
		Instant instant = Instant.parse("2026-10-27T22:30:00Z");
		Clock clock = new ClockConfig().clock(new MockEnvironment());

		assertThat(LocalDate.now(Clock.fixed(instant, clock.getZone()))).isEqualTo(LocalDate.of(2026, 10, 28));
		assertThat(LocalDate.now(Clock.fixed(instant, ZoneId.of("UTC")))).isEqualTo(LocalDate.of(2026, 10, 27));
	}
}
