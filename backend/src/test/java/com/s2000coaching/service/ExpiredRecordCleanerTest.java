package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.repository.CoachSessionRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpiredRecordCleanerTest {

	@Mock
	private MemberSessionRepository memberSessionRepository;

	@Mock
	private CoachSessionRepository coachSessionRepository;

	@Mock
	private MemberVerificationCodeRepository codeRepository;

	private ExpiredRecordCleaner cleanerWithRetention(Duration retention) {
		return new ExpiredRecordCleaner(memberSessionRepository, coachSessionRepository, codeRepository,
				retention);
	}

	@Test
	void shouldDeleteSessionsThatHaveAlreadyExpired() {
		var cleaner = cleanerWithRetention(Duration.ofDays(7));

		cleaner.purge();

		var memberCutoff = ArgumentCaptor.forClass(Instant.class);
		verify(memberSessionRepository).deleteByExpiresAtBefore(memberCutoff.capture());
		verify(coachSessionRepository).deleteByExpiresAtBefore(any());
		assertThat(memberCutoff.getValue()).isBetween(Instant.now().minusSeconds(30), Instant.now());
	}

	@Test
	void shouldKeepCodesForTheConfiguredRetention() {
		var cleaner = cleanerWithRetention(Duration.ofDays(7));

		cleaner.purge();

		var cutoff = ArgumentCaptor.forClass(Instant.class);
		verify(codeRepository).deleteByCreatedAtBefore(cutoff.capture());
		assertThat(cutoff.getValue()).isCloseTo(Instant.now().minus(Duration.ofDays(7)),
				within(Duration.ofSeconds(30)));
	}

	/**
	 * A short retention would delete codes the rate limit still counts, handing the caller a
	 * fresh hourly allowance — the cleanup must not be able to weaken the limit.
	 */
	@Test
	void shouldRefuseARetentionShorterThanTheRateLimitWindow() {
		var cleaner = cleanerWithRetention(Duration.ofMinutes(20));

		cleaner.purge();

		var cutoff = ArgumentCaptor.forClass(Instant.class);
		verify(codeRepository).deleteByCreatedAtBefore(cutoff.capture());
		assertThat(cutoff.getValue()).isBefore(Instant.now().minus(Duration.ofHours(1)));
	}

	@Test
	void shouldNotFailWhenThereIsNothingToDelete() {
		when(memberSessionRepository.deleteByExpiresAtBefore(any())).thenReturn(0);
		when(coachSessionRepository.deleteByExpiresAtBefore(any())).thenReturn(0);
		when(codeRepository.deleteByCreatedAtBefore(any())).thenReturn(0);

		cleanerWithRetention(Duration.ofDays(7)).purge();
	}
}
