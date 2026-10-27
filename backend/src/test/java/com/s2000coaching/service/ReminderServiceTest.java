package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.mail.NotificationMailSender;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MembershipRepository;
import com.s2000coaching.repository.ReminderLogRepository;
import com.s2000coaching.web.dto.AttentionResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

	// A Tuesday, so no weekly check-in reminder muddies the membership/measurement cases.
	private static final Instant NOW = Instant.parse("2026-10-27T09:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private MeasurementRepository measurementRepository;
	@Mock
	private CheckInRepository checkInRepository;
	@Mock
	private MembershipRepository membershipRepository;
	@Mock
	private ReminderLogRepository reminderLogRepository;
	@Mock
	private CoachAccountRepository coachAccountRepository;
	@Mock
	private AttentionService attentionService;
	@Mock
	private NotificationMailSender mailSender;

	private Member member;

	@BeforeEach
	void setUp() {
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet Yılmaz", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.parse("2026-01-05T09:00:00Z"));
	}

	private ReminderService service(boolean enabled) {
		return new ReminderService(memberRepository, measurementRepository, checkInRepository, membershipRepository,
				reminderLogRepository, coachAccountRepository, attentionService, mailSender,
				Clock.fixed(NOW, ZoneOffset.UTC), "https://s2000.example/", enabled);
	}

	private void oneMemberWithAMembershipEndingInAWeek() {
		when(mailSender.delivers()).thenReturn(true);
		when(memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE)).thenReturn(List.of(member));
		when(measurementRepository.latestMeasuredOnPerMember())
				.thenReturn(List.<Object[]>of(new Object[] {member.getId(), TODAY}));
		when(checkInRepository.latestWeekPerMember()).thenReturn(List.of());
		when(membershipRepository.latestEndPerMember())
				.thenReturn(List.<Object[]>of(new Object[] {member.getId(), TODAY.plusDays(7)}));
		when(reminderLogRepository.allSent()).thenReturn(List.of());
		when(attentionService.load()).thenReturn(new AttentionResponse(List.of(), List.of()));
	}

	/** Without SMTP nothing may be recorded as sent, or turning SMTP on later would find it "done". */
	@Test
	void shouldDoNothingWhileTheMailIsOnlyBeingLogged() {
		when(mailSender.delivers()).thenReturn(false);

		assertThat(service(true).run()).isZero();

		verify(memberRepository, never()).findAllByStatusOrderByCreatedAtDesc(any());
		verify(reminderLogRepository, never()).save(any());
	}

	@Test
	void shouldDoNothingWhenSwitchedOff() {
		assertThat(service(false).run()).isZero();
		verify(mailSender, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void shouldSendADueReminderAndRecordIt() {
		oneMemberWithAMembershipEndingInAWeek();
		when(mailSender.send(eq("a@example.com"), anyString(), anyString())).thenReturn(true);

		assertThat(service(true).run()).isEqualTo(1);

		verify(reminderLogRepository).save(any());
	}

	@Test
	void shouldNotSendTheSameReminderTwice() {
		oneMemberWithAMembershipEndingInAWeek();
		String occasion = TODAY.plusDays(7).toString();
		when(reminderLogRepository.allSent())
				.thenReturn(List.<Object[]>of(new Object[] {member.getId(), "MEMBERSHIP_ENDING", occasion}));

		assertThat(service(true).run()).isZero();

		verify(mailSender, never()).send(anyString(), anyString(), anyString());
	}

	/** A failed send is not logged, so tomorrow's run tries again. */
	@Test
	void shouldNotRecordAReminderTheMailServerRefused() {
		oneMemberWithAMembershipEndingInAWeek();
		when(mailSender.send(anyString(), anyString(), anyString())).thenReturn(false);

		assertThat(service(true).run()).isZero();

		verify(reminderLogRepository, never()).save(any());
	}

	@Test
	void shouldMailTheCoachADigestOnlyWhenThereIsSomethingToSay() {
		when(mailSender.delivers()).thenReturn(true);
		when(memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE)).thenReturn(List.of());
		when(measurementRepository.latestMeasuredOnPerMember()).thenReturn(List.of());
		when(checkInRepository.latestWeekPerMember()).thenReturn(List.of());
		when(membershipRepository.latestEndPerMember()).thenReturn(List.of());
		when(reminderLogRepository.allSent()).thenReturn(List.of());
		when(attentionService.load()).thenReturn(new AttentionResponse(List.of(), List.of()));

		assertThat(service(true).run()).isZero();

		verify(coachAccountRepository, never()).findAll();
	}

	@Test
	void shouldSendTheCoachADigestWhenSomeoneNeedsAttention() {
		when(mailSender.delivers()).thenReturn(true);
		when(memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE)).thenReturn(List.of());
		when(measurementRepository.latestMeasuredOnPerMember()).thenReturn(List.of());
		when(checkInRepository.latestWeekPerMember()).thenReturn(List.of());
		when(membershipRepository.latestEndPerMember()).thenReturn(List.of());
		when(reminderLogRepository.allSent()).thenReturn(List.of());
		var item = new AttentionScorer.Item(member.getId(), "Ahmet Yılmaz", 4,
				List.of(new AttentionScorer.Reason("MESSAGE", "Cevap bekleyen mesajı var", 4)));
		when(attentionService.load()).thenReturn(new AttentionResponse(List.of(item), List.of()));
		var coach = new CoachAccount(UUID.randomUUID(), "koc@example.com", "hash", "Koç", Instant.now());
		when(coachAccountRepository.findAll()).thenReturn(List.of(coach));
		when(mailSender.send(eq("koc@example.com"), eq("S2000 günlük özet"), anyString())).thenReturn(true);

		assertThat(service(true).run()).isEqualTo(1);
	}
}
