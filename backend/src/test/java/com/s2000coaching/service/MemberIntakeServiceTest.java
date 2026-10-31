package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberIntake;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MemberIntakeRepository;
import com.s2000coaching.web.dto.UpsertIntakeRequest;
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
class MemberIntakeServiceTest {

	private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

	@Mock
	private MemberIntakeRepository intakeRepository;

	@Mock
	private MemberService memberService;

	private MemberIntakeService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new MemberIntakeService(intakeRepository, memberService);
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	private static UpsertIntakeRequest request(int calories) {
		return new UpsertIntakeRequest(DAY, calories, 180, 250, 70, null, "  iyi gün  ");
	}

	@Test
	void shouldRefuseWithoutHealthDataConsent() {
		assertThatThrownBy(() -> service.save(member, request(2500))).isInstanceOf(BadRequestException.class);

		verify(intakeRepository, never()).save(any());
	}

	@Test
	void shouldCreateTheDayAndTrimTheNote() {
		member.grantHealthDataConsent(Instant.now());
		when(intakeRepository.findByMemberIdAndConsumedOn(member.getId(), DAY)).thenReturn(Optional.empty());
		when(intakeRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		var response = service.save(member, request(2500));

		assertThat(response.calories()).isEqualTo(2500);
		assertThat(response.proteinG()).isEqualTo(180);
		assertThat(response.note()).isEqualTo("iyi gün");
	}

	@Test
	void shouldKeepTheWaterDrunkAndAllowLeavingItOut() {
		member.grantHealthDataConsent(Instant.now());
		when(intakeRepository.findByMemberIdAndConsumedOn(member.getId(), DAY)).thenReturn(Optional.empty());
		when(intakeRepository.save(any())).thenAnswer(call -> call.getArgument(0));

		var withWater = service.save(member, new UpsertIntakeRequest(DAY, 2400, null, null, null, null, null, new BigDecimal("3.5")));
		var without = service.save(member, request(2400));

		assertThat(withWater.waterLiters()).isEqualByComparingTo("3.5");
		assertThat(without.waterLiters()).isNull();
	}

	/** Correcting a day must replace it, not add a second row the analysis would double count. */
	@Test
	void shouldReplaceAnExistingDayInsteadOfAddingOne() {
		member.grantHealthDataConsent(Instant.now());
		var existing = new MemberIntake(UUID.randomUUID(), member, DAY, 2000, Instant.now());
		when(intakeRepository.findByMemberIdAndConsumedOn(member.getId(), DAY)).thenReturn(Optional.of(existing));

		service.save(member, request(2600));

		assertThat(existing.getCalories()).isEqualTo(2600);
		verify(intakeRepository, never()).save(any());
	}

	@Test
	void shouldNotLetAMemberDeleteSomeoneElsesDay() {
		var other = new Member(UUID.randomUUID(), "b@example.com", "Başka", "+905551112244", 170,
				new BigDecimal("70"), "x", MemberStatus.ACTIVE, Instant.now());
		var theirs = new MemberIntake(UUID.randomUUID(), other, DAY, 2000, Instant.now());
		when(intakeRepository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

		assertThatThrownBy(() -> service.deleteOwn(member, theirs.getId())).isInstanceOf(NotFoundException.class);
		verify(intakeRepository, never()).delete(any());
	}

	@Test
	void shouldEraseEverythingOfAMember() {
		UUID id = UUID.randomUUID();

		service.eraseAllOf(id);

		verify(intakeRepository).deleteAllByMemberId(id);
	}
}
