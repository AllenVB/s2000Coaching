package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.WeeklyNutrition;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.WeeklyNutritionRepository;
import com.s2000coaching.service.WeeklyNutritionService.PhotoUpload;
import com.s2000coaching.storage.PhotoStorage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyNutritionServiceTest {

	/** Friday 30 October 2026: this week began on Monday 26 October. */
	private static final LocalDate THIS_MONDAY = LocalDate.of(2026, 10, 26);
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, 1, 2, 3 };

	@Mock
	private WeeklyNutritionRepository repository;
	@Mock
	private PhotoStorage photoStorage;
	@Mock
	private MemberService memberService;

	private WeeklyNutritionService service;
	private Member member;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-10-30T09:00:00Z"), ZoneId.of("Europe/Istanbul"));
		service = new WeeklyNutritionService(repository, photoStorage, memberService, clock, 8L * 1024 * 1024);
		member = new Member(UUID.randomUUID(), "uye@example.com", "Ahmet", "", 184, new BigDecimal("80"), "5 yıl",
				MemberStatus.ACTIVE, Instant.now());
		member.grantHealthDataConsent(Instant.now());
	}

	private void noEarlierWeek(LocalDate week) {
		when(repository.findByMemberIdAndWeekStart(member.getId(), week)).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void shouldSaveAWeekAndWorkOutTheGrams() {
		noEarlierWeek(THIS_MONDAY);

		var response = service.save(member, THIS_MONDAY, 14_000, 50, 30, 20, null, false);

		assertThat(response.weekEnd()).isEqualTo(LocalDate.of(2026, 11, 1));
		// 14 000 kcal: carbohydrate 7 000 kcal / 4, fat 4 200 kcal / 9, protein 2 800 kcal / 4.
		assertThat(response.weekly().carbsG()).isEqualByComparingTo("1750.0");
		assertThat(response.weekly().fatG()).isEqualByComparingTo("466.7");
		assertThat(response.weekly().proteinG()).isEqualByComparingTo("700.0");
		assertThat(response.dailyAverage().calories()).isEqualByComparingTo("2000.0");
		assertThat(response.dailyAverage().carbsG()).isEqualByComparingTo("250.0");
		assertThat(response.dailyAverage().fatG()).isEqualByComparingTo("66.7");
		assertThat(response.dailyAverage().proteinG()).isEqualByComparingTo("100.0");
		assertThat(response.hasPhoto()).isFalse();
	}

	@Test
	void shouldRefuseWhenThePercentagesDoNotAddUpToOneHundred() {
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 14_000, 50, 30, 10, null, false))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("toplamı 100")
				.hasMessageContaining("90");
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 14_000, 60, 30, 20, null, false))
				.hasMessageContaining("110");
		verify(repository, never()).save(any());
	}

	@Test
	void shouldRefuseNegativeOrOversizedPercentagesEvenWhenTheyAddUp() {
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 14_000, 120, -20, 0, null, false))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("0 ile 100");
	}

	@Test
	void shouldRefuseCaloriesOutOfRange() {
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 0, 50, 30, 20, null, false))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 100_001, 50, 30, 20, null, false))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldOnlyAcceptAWeekThatStartsOnAMonday() {
		assertThatThrownBy(() -> service.save(member, LocalDate.of(2026, 10, 27), 14_000, 50, 30, 20, null, false))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("pazartesi");
		assertThatThrownBy(() -> service.save(member, LocalDate.of(2026, 10, 25), 14_000, 50, 30, 20, null, false))
				.hasMessageContaining("pazartesi");
	}

	@Test
	void shouldRefuseAWeekThatHasNotStartedOrIsTooOld() {
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY.plusWeeks(1), 14_000, 50, 30, 20, null, false))
				.hasMessageContaining("başlamamış");
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY.minusWeeks(157), 14_000, 50, 30, 20, null, false))
				.hasMessageContaining("eski");
	}

	@Test
	void shouldAcceptThisWeekAndLastWeek() {
		when(repository.findByMemberIdAndWeekStart(any(), any())).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

		assertThat(service.save(member, THIS_MONDAY, 14_000, 50, 30, 20, null, false).weekStart()).isEqualTo(THIS_MONDAY);
		assertThat(service.save(member, THIS_MONDAY.minusWeeks(1), 14_000, 50, 30, 20, null, false).weekStart())
				.isEqualTo(LocalDate.of(2026, 10, 19));
	}

	@Test
	void shouldNotWriteWithoutHealthDataConsent() {
		Member noConsent = new Member(UUID.randomUUID(), "x@example.com", "X", "", 180, new BigDecimal("70"), "yok",
				MemberStatus.ACTIVE, Instant.now());

		assertThatThrownBy(() -> service.save(noConsent, THIS_MONDAY, 14_000, 50, 30, 20, null, false))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("onay");
		verify(repository, never()).save(any());
	}

	@Test
	void shouldStoreAPhotoAndKeepOnlyItsKey() {
		noEarlierWeek(THIS_MONDAY);
		when(photoStorage.store(JPEG, "image/jpeg")).thenReturn("key-1");

		var response = service.save(member, THIS_MONDAY, 14_000, 50, 30, 20, new PhotoUpload("image/jpeg", JPEG), false);

		assertThat(response.hasPhoto()).isTrue();
	}

	@Test
	void shouldRefuseAPhotoThatIsNotAnAllowedImage() {
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 14_000, 50, 30, 20,
				new PhotoUpload("image/svg+xml", JPEG), false)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 14_000, 50, 30, 20,
				new PhotoUpload("text/html", JPEG), false)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.save(member, THIS_MONDAY, 14_000, 50, 30, 20,
				new PhotoUpload("image/png", new byte[0]), false)).isInstanceOf(BadRequestException.class);
		verify(photoStorage, never()).store(any(), any());
	}

	@Test
	void shouldReplaceAnExistingWeekAndItsPhoto() {
		var existing = new WeeklyNutrition(UUID.randomUUID(), member, THIS_MONDAY, 10_000, 40, 40, 20, Instant.now());
		existing.attachPhoto("old-key", "image/png", 10);
		when(repository.findByMemberIdAndWeekStart(member.getId(), THIS_MONDAY)).thenReturn(Optional.of(existing));
		when(photoStorage.store(JPEG, "image/jpeg")).thenReturn("new-key");

		var response = service.save(member, THIS_MONDAY, 14_000, 50, 30, 20, new PhotoUpload("image/jpeg", JPEG), false);

		assertThat(response.calories()).isEqualTo(14_000);
		assertThat(existing.getPhotoKey()).isEqualTo("new-key");
		verify(repository, never()).save(any());
		verify(photoStorage).delete("old-key");
	}

	@Test
	void shouldKeepTheEarlierPhotoWhenNoneIsSent() {
		var existing = new WeeklyNutrition(UUID.randomUUID(), member, THIS_MONDAY, 10_000, 40, 40, 20, Instant.now());
		existing.attachPhoto("old-key", "image/png", 10);
		when(repository.findByMemberIdAndWeekStart(member.getId(), THIS_MONDAY)).thenReturn(Optional.of(existing));

		service.save(member, THIS_MONDAY, 12_000, 50, 30, 20, null, false);

		assertThat(existing.getPhotoKey()).isEqualTo("old-key");
		verify(photoStorage, never()).delete(any());
	}

	@Test
	void shouldDropThePhotoWhenAskedTo() {
		var existing = new WeeklyNutrition(UUID.randomUUID(), member, THIS_MONDAY, 10_000, 40, 40, 20, Instant.now());
		existing.attachPhoto("old-key", "image/png", 10);
		when(repository.findByMemberIdAndWeekStart(member.getId(), THIS_MONDAY)).thenReturn(Optional.of(existing));

		var response = service.save(member, THIS_MONDAY, 10_000, 40, 40, 20, null, true);

		assertThat(response.hasPhoto()).isFalse();
		verify(photoStorage).delete("old-key");
	}

	@Test
	void shouldNotLetOneMemberReadOrDeleteAnothersWeek() {
		Member other = new Member(UUID.randomUUID(), "baska@example.com", "B", "", 170, new BigDecimal("60"), "yok",
				MemberStatus.ACTIVE, Instant.now());
		var theirs = new WeeklyNutrition(UUID.randomUUID(), other, THIS_MONDAY, 10_000, 40, 40, 20, Instant.now());
		theirs.attachPhoto("k", "image/png", 10);
		when(repository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

		assertThatThrownBy(() -> service.readOwnPhoto(member, theirs.getId())).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> service.deleteOwn(member, theirs.getId())).isInstanceOf(NotFoundException.class);
		verify(repository, never()).delete(any());
	}

	@Test
	void shouldDeleteAWeekAndItsPhotoEvenWithoutConsent() {
		Member noConsent = new Member(UUID.randomUUID(), "x@example.com", "X", "", 180, new BigDecimal("70"), "yok",
				MemberStatus.ACTIVE, Instant.now());
		var mine = new WeeklyNutrition(UUID.randomUUID(), noConsent, THIS_MONDAY, 10_000, 40, 40, 20, Instant.now());
		mine.attachPhoto("k", "image/png", 10);
		when(repository.findById(mine.getId())).thenReturn(Optional.of(mine));

		service.deleteOwn(noConsent, mine.getId());

		verify(repository).delete(mine);
		verify(photoStorage).delete("k");
	}

	@Test
	void shouldEraseEveryWeekAndFileOfAMember() {
		var a = new WeeklyNutrition(UUID.randomUUID(), member, THIS_MONDAY, 10_000, 40, 40, 20, Instant.now());
		a.attachPhoto("ka", "image/png", 1);
		var b = new WeeklyNutrition(UUID.randomUUID(), member, THIS_MONDAY.minusWeeks(1), 10_000, 40, 40, 20, Instant.now());
		when(repository.findAllByMemberIdOrderByWeekStartDesc(member.getId())).thenReturn(List.of(a, b));

		service.eraseAllOf(member.getId());

		verify(photoStorage).delete("ka");
		verify(photoStorage, never()).delete(eq("kb"));
		verify(repository).deleteAllByMemberId(member.getId());
	}
}
