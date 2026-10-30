package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.MonthlyRecord;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MonthlyRecordRepository;
import com.s2000coaching.web.dto.UpsertMonthlyRecordRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyRecordServiceTest {

	@Mock
	private MonthlyRecordRepository recordRepository;
	@Mock
	private MemberService memberService;

	private MonthlyRecordService service;
	private Member member;

	@BeforeEach
	void setUp() {
		// The 28th of October 2026, Istanbul.
		Clock clock = Clock.fixed(Instant.parse("2026-10-28T09:00:00Z"), ZoneId.of("Europe/Istanbul"));
		service = new MonthlyRecordService(recordRepository, memberService, clock);
		member = new Member(UUID.randomUUID(), "uye@example.com", "Ahmet", "", 184, new BigDecimal("80"), "5 yıl",
				MemberStatus.ACTIVE, Instant.now());
		member.grantHealthDataConsent(Instant.now());
	}

	private static UpsertMonthlyRecordRequest table(String period, String weight, String waist) {
		return new UpsertMonthlyRecordRequest(period, new BigDecimal("184"), weight == null ? null : new BigDecimal(weight),
				null, null, null, null, waist == null ? null : new BigDecimal(waist), null, null);
	}

	@Test
	void shouldSaveAMonthAsATable() {
		when(memberService.requireMember(member.getId())).thenReturn(member);
		when(recordRepository.findByMemberIdAndPeriod(member.getId(), "2026-10")).thenReturn(Optional.empty());
		when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var saved = service.create(member.getId(), table("2026-10", "80.4", "86"));

		assertThat(saved.period()).isEqualTo("2026-10");
		assertThat(saved.heightCm()).isEqualByComparingTo("184");
		assertThat(saved.weightKg()).isEqualByComparingTo("80.4");
		assertThat(saved.waistCm()).isEqualByComparingTo("86");
		assertThat(saved.chestCm()).isNull();
	}

	@Test
	void shouldRefuseAMonthThatAlreadyHasATableAndPointToEditing() {
		when(memberService.requireMember(member.getId())).thenReturn(member);
		when(recordRepository.findByMemberIdAndPeriod(member.getId(), "2026-10"))
				.thenReturn(Optional.of(new MonthlyRecord(UUID.randomUUID(), member, "2026-10", Instant.now())));

		assertThatThrownBy(() -> service.create(member.getId(), table("2026-10", "80", null)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("10/2026")
				.hasMessageContaining("düzenle");
		verify(recordRepository, never()).save(any());
	}

	@Test
	void shouldRefuseAFutureMonthButAcceptThisOneAndEarlierOnes() {
		when(memberService.requireMember(member.getId())).thenReturn(member);
		when(recordRepository.findByMemberIdAndPeriod(any(), any())).thenReturn(Optional.empty());
		when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThatThrownBy(() -> service.create(member.getId(), table("2026-11", "80", null)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("Gelecekteki");
		service.create(member.getId(), table("2026-10", "80", null));
		service.create(member.getId(), table("2025-01", "82", null));
	}

	@Test
	void shouldNotSaveAnEmptyTableOrOneWithoutAMonth() {
		when(memberService.requireMember(member.getId())).thenReturn(member);

		assertThatThrownBy(() -> service.create(member.getId(),
				new UpsertMonthlyRecordRequest("2026-10", null, null, null, null, null, null, null, null, null)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("En az bir değer");
		assertThatThrownBy(() -> service.create(member.getId(), table(null, "80", null)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("Ay seçin");
	}

	@Test
	void shouldWriteNothingBeforeTheMemberHasConsentedToHealthData() {
		Member noConsent = new Member(UUID.randomUUID(), "b@example.com", "Bora", "", 180, new BigDecimal("70"), "yok",
				MemberStatus.ACTIVE, Instant.now());
		when(memberService.requireMember(noConsent.getId())).thenReturn(noConsent);

		assertThatThrownBy(() -> service.create(noConsent.getId(), table("2026-10", "70", null)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("onay");
		verify(recordRepository, never()).save(any());
	}

	@Test
	void shouldReplaceTheWholeTableOnEditAndKeepTheMonth() {
		MonthlyRecord record = new MonthlyRecord(UUID.randomUUID(), member, "2026-09", Instant.now());
		record.setFigures(new BigDecimal("184"), new BigDecimal("80"), null, null, null, null, new BigDecimal("90"), null,
				null, Instant.now());
		when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));

		var edited = service.update(member.getId(), record.getId(), table("2030-01", "79.2", null));

		assertThat(edited.period()).isEqualTo("2026-09");
		assertThat(edited.weightKg()).isEqualByComparingTo("79.2");
		// The form always sends the whole table: a figure left empty is cleared, not kept.
		assertThat(edited.waistCm()).isNull();
	}

	@Test
	void shouldAnswerAnotherMembersRowLikeAMissingOne() {
		Member other = new Member(UUID.randomUUID(), "o@example.com", "Oya", "", 170, new BigDecimal("60"), "yok",
				MemberStatus.ACTIVE, Instant.now());
		MonthlyRecord record = new MonthlyRecord(UUID.randomUUID(), other, "2026-09", Instant.now());
		when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));

		assertThatThrownBy(() -> service.update(member.getId(), record.getId(), table("2026-09", "80", null)))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> service.delete(member.getId(), record.getId()))
				.isInstanceOf(NotFoundException.class);
		verify(recordRepository, never()).delete(any());
	}

	@Test
	void shouldAlwaysAllowDeletingOwnDataEvenWithoutConsent() {
		member = new Member(UUID.randomUUID(), "c@example.com", "Can", "", 180, new BigDecimal("70"), "yok",
				MemberStatus.ACTIVE, Instant.now());
		MonthlyRecord record = new MonthlyRecord(UUID.randomUUID(), member, "2026-09", Instant.now());
		when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));

		service.delete(member.getId(), record.getId());

		verify(recordRepository).delete(record);
	}

	@Test
	void shouldLetTheCoachEditAndDeleteWithoutKnowingTheOwner() {
		MonthlyRecord record = new MonthlyRecord(UUID.randomUUID(), member, "2026-09", Instant.now());
		when(recordRepository.findById(record.getId())).thenReturn(Optional.of(record));

		assertThat(service.updateAsCoach(record.getId(), table("2026-09", "81", null)).weightKg()).isEqualByComparingTo("81");
		service.deleteAsCoach(record.getId());

		verify(recordRepository).delete(record);
	}

	@Test
	void shouldShowTheMonthTheWayTheMemberWritesIt() {
		assertThat(MonthlyRecordService.label("2026-10")).isEqualTo("10/2026");
	}
}
