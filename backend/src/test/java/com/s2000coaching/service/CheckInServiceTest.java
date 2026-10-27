package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CheckIn;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.web.dto.UpsertCheckInRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

	// A Tuesday: its week starts on Monday 2026-10-26.
	private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 27);
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

	@Mock
	private CheckInRepository checkInRepository;

	@Mock
	private MemberService memberService;

	private CheckInService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new CheckInService(checkInRepository, memberService,
				Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC));
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	private static UpsertCheckInRequest request(LocalDate weekOf) {
		return new UpsertCheckInRequest(weekOf, 4, 2, 3, 3, 90, "  iyi hafta  ");
	}

	@Test
	void shouldRefuseWithoutHealthDataConsent() {
		assertThatThrownBy(() -> service.save(member, request(TUESDAY))).isInstanceOf(BadRequestException.class);

		verify(checkInRepository, never()).save(any());
	}

	@Test
	void shouldKeyTheCheckInByTheMondayOfTheWeek() {
		member.grantHealthDataConsent(Instant.now());
		when(checkInRepository.findByMemberIdAndWeekStart(member.getId(), MONDAY)).thenReturn(Optional.empty());
		when(checkInRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		var response = service.save(member, request(TUESDAY));

		assertThat(response.weekStart()).isEqualTo(MONDAY);
		assertThat(response.note()).isEqualTo("iyi hafta");
		assertThat(CheckInService.weekStart(LocalDate.of(2026, 11, 1))).isEqualTo(MONDAY); // a Sunday
		assertThat(CheckInService.weekStart(MONDAY)).isEqualTo(MONDAY);
	}

	/** Changed answers are something new for the coach, so an old reply must not stand over them. */
	@Test
	void shouldClearTheCoachReplyWhenTheWeekIsSubmittedAgain() {
		member.grantHealthDataConsent(Instant.now());
		var existing = new CheckIn(UUID.randomUUID(), member, MONDAY, Instant.now());
		existing.fill(3, 3, 3, 3, null, null, Instant.now());
		existing.reply("Süper gidiyorsun", Instant.now());
		when(checkInRepository.findByMemberIdAndWeekStart(member.getId(), MONDAY)).thenReturn(Optional.of(existing));

		var response = service.save(member, request(TUESDAY));

		assertThat(response.coachReply()).isNull();
		assertThat(response.coachRepliedAt()).isNull();
		assertThat(response.sleep()).isEqualTo(4);
		verify(checkInRepository, never()).save(any());
	}

	@Test
	void shouldStoreTheCoachReply() {
		var checkIn = new CheckIn(UUID.randomUUID(), member, MONDAY, Instant.now());
		when(checkInRepository.findById(checkIn.getId())).thenReturn(Optional.of(checkIn));

		var response = service.reply(checkIn.getId(), "  Uykuya dikkat et  ");

		assertThat(response.coachReply()).isEqualTo("Uykuya dikkat et");
		assertThat(response.coachRepliedAt()).isEqualTo(Instant.parse("2026-10-27T09:00:00Z"));
	}

	@Test
	void shouldFailToReplyToAnUnknownCheckIn() {
		UUID unknown = UUID.randomUUID();
		when(checkInRepository.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.reply(unknown, "x")).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldEraseEverythingOfAMember() {
		service.eraseAllOf(member.getId());

		verify(checkInRepository).deleteAllByMemberId(member.getId());
	}
}
