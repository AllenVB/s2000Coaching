package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberActivity;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MemberActivityRepository;
import com.s2000coaching.web.dto.UpsertActivityRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberActivityServiceTest {

	private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

	@Mock
	private MemberActivityRepository activityRepository;

	@Mock
	private MemberService memberService;

	private MemberActivityService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new MemberActivityService(activityRepository, memberService);
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	@Test
	void shouldRefuseWithoutHealthDataConsent() {
		assertThatThrownBy(() -> service.save(member, new UpsertActivityRequest(DAY, 9000, 30)))
				.isInstanceOf(BadRequestException.class);
		verify(activityRepository, never()).save(any());
	}

	@Test
	void shouldRefuseAnEntryWithNeitherStepsNorCardio() {
		member.grantHealthDataConsent(Instant.now());

		assertThatThrownBy(() -> service.save(member, new UpsertActivityRequest(DAY, null, null)))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldReplaceAnExistingDayInsteadOfAddingASecondRow() {
		member.grantHealthDataConsent(Instant.now());
		var existing = new MemberActivity(UUID.randomUUID(), member, DAY, Instant.now());
		existing.record(5000, null, Instant.now());
		when(activityRepository.findByMemberIdAndPerformedOn(member.getId(), DAY)).thenReturn(Optional.of(existing));

		var response = service.save(member, new UpsertActivityRequest(DAY, 9000, 25));

		assertThat(response.steps()).isEqualTo(9000);
		assertThat(response.cardioMinutes()).isEqualTo(25);
		verify(activityRepository, never()).save(any());
	}

	@Test
	void shouldNotLetAMemberDeleteSomeoneElsesRow() {
		var other = new Member(UUID.randomUUID(), "b@example.com", "Başka", "+905551112244", 170,
				new BigDecimal("70"), "x", MemberStatus.ACTIVE, Instant.now());
		var theirs = new MemberActivity(UUID.randomUUID(), other, DAY, Instant.now());
		when(activityRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

		assertThatThrownBy(() -> service.deleteOwn(member, theirs.getId())).isInstanceOf(NotFoundException.class);
		verify(activityRepository, never()).delete(any());
	}
}
