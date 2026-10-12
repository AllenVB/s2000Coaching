package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Measurement;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.web.dto.UpsertMeasurementRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	@Mock
	private MeasurementRepository measurementRepository;

	@Mock
	private MemberService memberService;

	private MeasurementService measurementService;
	private Member member;

	@BeforeEach
	void setUp() {
		measurementService = new MeasurementService(measurementRepository, memberService);
		member = new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.ACTIVE, Instant.now());
	}

	private static UpsertMeasurementRequest request(String weight) {
		return new UpsertMeasurementRequest(TODAY, new BigDecimal(weight), new BigDecimal("84.0"), null, null,
				null, null, "  iyi hissediyorum  ");
	}

	@Nested
	class Saving {

		@Test
		void shouldRefuseToWriteWithoutHealthDataConsent() {
			assertThatThrownBy(() -> measurementService.save(member, request("80.0")))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("sağlık verisi işleme onayını");

			verify(measurementRepository, never()).save(any());
		}

		@Test
		void shouldStoreACheckInOnceConsentIsGiven() {
			member.grantHealthDataConsent(Instant.now());
			when(measurementRepository.findByMemberIdAndMeasuredOn(member.getId(), TODAY))
					.thenReturn(Optional.empty());
			when(measurementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = measurementService.save(member, request("80.0"));

			assertThat(response.weightKg()).isEqualByComparingTo("80.0");
			assertThat(response.waistCm()).isEqualByComparingTo("84.0");
			assertThat(response.note()).isEqualTo("iyi hissediyorum");
			assertThat(response.measuredOn()).isEqualTo(TODAY);
		}

		@Test
		void shouldOverwriteTheSameDayInsteadOfAddingASecondRow() {
			member.grantHealthDataConsent(Instant.now());
			var existing = new Measurement(UUID.randomUUID(), member, TODAY, new BigDecimal("82.0"),
					Instant.now());
			when(measurementRepository.findByMemberIdAndMeasuredOn(member.getId(), TODAY))
					.thenReturn(Optional.of(existing));

			var response = measurementService.save(member, request("80.0"));

			assertThat(response.id()).isEqualTo(existing.getId());
			assertThat(existing.getWeightKg()).isEqualByComparingTo("80.0");
			verify(measurementRepository, never()).save(any());
		}

		@Test
		void shouldStoreABlankNoteAsNull() {
			member.grantHealthDataConsent(Instant.now());
			when(measurementRepository.findByMemberIdAndMeasuredOn(member.getId(), TODAY))
					.thenReturn(Optional.empty());
			when(measurementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = measurementService.save(member, new UpsertMeasurementRequest(
					TODAY, new BigDecimal("80.0"), null, null, null, null, null, "   "));

			assertThat(response.note()).isNull();
		}
	}

	@Nested
	class Deleting {

		@Test
		void shouldDeleteTheMembersOwnRow() {
			var own = new Measurement(UUID.randomUUID(), member, TODAY, new BigDecimal("80.0"), Instant.now());
			when(measurementRepository.findById(own.getId())).thenReturn(Optional.of(own));

			measurementService.deleteOwn(member, own.getId());

			verify(measurementRepository).delete(own);
		}

		@Test
		void shouldHideAnotherMembersRowBehindA404() {
			Member other = new Member(UUID.randomUUID(), "baska@example.com", "Başka Üye", "+905550001122",
					170, new BigDecimal("70.00"), "Koşu", MemberStatus.ACTIVE, Instant.now());
			var foreign = new Measurement(UUID.randomUUID(), other, TODAY, new BigDecimal("70.0"),
					Instant.now());
			when(measurementRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

			assertThatThrownBy(() -> measurementService.deleteOwn(member, foreign.getId()))
					.isInstanceOf(NotFoundException.class)
					.hasMessageContaining("Ölçüm bulunamadı");

			verify(measurementRepository, never()).delete(any());
		}
	}

	@Test
	void shouldCheckTheMemberExistsBeforeListingAsCoach() {
		UUID unknown = UUID.randomUUID();
		when(memberService.requireMember(unknown)).thenThrow(new NotFoundException("Üye bulunamadı"));

		assertThatThrownBy(() -> measurementService.listForMemberAsCoach(unknown))
				.isInstanceOf(NotFoundException.class);
		verify(measurementRepository, never()).findAllByMemberIdOrderByMeasuredOnDesc(any());
	}
}
