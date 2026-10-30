package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.WeeklyNutritionRepository;
import com.s2000coaching.service.WeeklyNutritionService.PhotoUpload;
import com.s2000coaching.storage.PhotoStorage;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Against the real schema: the NOT NULL columns, the one-row-per-week constraint and the
 * "percentages add up to 100" check live in the database, where a mocked repository cannot see them.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WeeklyNutritionIntegrationTest {

	@Autowired
	private WeeklyNutritionService service;
	@Autowired
	private WeeklyNutritionRepository repository;
	@Autowired
	private MemberRepository memberRepository;
	@MockitoBean
	private PhotoStorage photoStorage;

	private static final LocalDate MONDAY = LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

	private Member newConsentedMember() {
		Member member = new Member(UUID.randomUUID(), UUID.randomUUID() + "@example.com", "Test Üye", "", 180,
				new BigDecimal("80.00"), "yok", MemberStatus.ACTIVE, Instant.now());
		member.assignMemberNumber(ThreadLocalRandom.current().nextInt(30_000, 49_999));
		member.grantHealthDataConsent(Instant.now());
		return memberRepository.save(member);
	}

	@Test
	void shouldSaveAWeekAndReplaceItInsteadOfAddingASecondRow() {
		Member member = newConsentedMember();
		when(photoStorage.store(any(), any())).thenReturn("key-1", "key-2");

		service.save(member, MONDAY, 14_000, 50, 30, 20, new PhotoUpload("image/png", new byte[] { 1 }), false);
		service.save(member, MONDAY, 15_000, 40, 40, 20, new PhotoUpload("image/png", new byte[] { 2 }), false);

		var rows = repository.findAllByMemberIdOrderByWeekStartDesc(member.getId());
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getCalories()).isEqualTo(15_000);
		assertThat(rows.get(0).getCarbsPercent()).isEqualTo(40);
		assertThat(rows.get(0).getPhotoKey()).isEqualTo("key-2");
	}

	@Test
	void shouldKeepADifferentWeekAsASeparateEntryNewestFirst() {
		Member member = newConsentedMember();

		service.save(member, MONDAY.minusWeeks(1), 14_000, 50, 30, 20, null, false);
		service.save(member, MONDAY, 12_000, 45, 35, 20, null, false);

		assertThat(service.list(member.getId())).extracting(r -> r.weekStart())
				.containsExactly(MONDAY, MONDAY.minusWeeks(1));
	}

	@Test
	void shouldLetTheDatabaseRefusePercentagesThatDoNotAddUp() {
		Member member = newConsentedMember();
		var bad = new com.s2000coaching.domain.WeeklyNutrition(UUID.randomUUID(), member, MONDAY, 14_000, 50, 30, 10,
				Instant.now());

		assertThatThrownBy(() -> repository.saveAndFlush(bad)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldEraseWeeksWhenTheMemberIsErased() {
		Member member = newConsentedMember();
		service.save(member, MONDAY, 14_000, 50, 30, 20, null, false);

		service.eraseAllOf(member.getId());

		assertThat(repository.findAllByMemberIdOrderByWeekStartDesc(member.getId())).isEmpty();
	}
}
