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
import com.s2000coaching.domain.VerificationPurpose;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CoachNoteRepository;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberSessionRepository;
import com.s2000coaching.repository.MemberVerificationCodeRepository;
import com.s2000coaching.repository.MessageRepository;
import com.s2000coaching.repository.NutritionPlanRepository;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.web.dto.CreateMemberRequest;
import com.s2000coaching.web.dto.UpdateMemberRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberVerificationCodeRepository codeRepository;

	@Mock
	private MemberSessionRepository sessionRepository;

	@Mock
	private CoachNoteRepository coachNoteRepository;

	@Mock
	private MeasurementRepository measurementRepository;

	@Mock
	private WorkoutLogRepository workoutLogRepository;

	@Mock
	private TrainingPlanRepository trainingPlanRepository;

	@Mock
	private NutritionPlanRepository nutritionPlanRepository;

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private VerificationCodeService verificationCodeService;

	private MemberService memberService;

	@BeforeEach
	void setUp() {
		memberService = new MemberService(memberRepository, codeRepository, sessionRepository,
				coachNoteRepository, measurementRepository, workoutLogRepository, trainingPlanRepository,
				nutritionPlanRepository, messageRepository, verificationCodeService);
	}

	private static CreateMemberRequest request(String email) {
		return new CreateMemberRequest(email, "  Ahmet Yılmaz ", " +905551112233 ", 182,
				new BigDecimal("84.50"), " 5 yıl fitness, 2 yıl powerlifting ");
	}

	private static Member pendingMember() {
		return new Member(UUID.randomUUID(), "sporcu@example.com", "Ahmet Yılmaz", "+905551112233", 182,
				new BigDecimal("84.50"), "5 yıl fitness", MemberStatus.PENDING_VERIFICATION, Instant.now());
	}

	@Nested
	class Creating {

		@Test
		void shouldCreatePendingMemberWithNormalizedFieldsAndMailACode() {
			when(memberRepository.existsByEmail("sporcu@example.com")).thenReturn(false);
			when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

			var response = memberService.createMember(request("  Sporcu@Example.COM "));

			assertThat(response.email()).isEqualTo("sporcu@example.com");
			assertThat(response.fullName()).isEqualTo("Ahmet Yılmaz");
			assertThat(response.phone()).isEqualTo("+905551112233");
			assertThat(response.sportsBackground()).isEqualTo("5 yıl fitness, 2 yıl powerlifting");
			assertThat(response.status()).isEqualTo(MemberStatus.PENDING_VERIFICATION);
			assertThat(response.verifiedAt()).isNull();
			verify(verificationCodeService).issueAndSend(any(Member.class), eq(VerificationPurpose.REGISTRATION));
		}

		@Test
		void shouldRejectAnEmailThatIsAlreadyRegistered() {
			when(memberRepository.existsByEmail("sporcu@example.com")).thenReturn(true);

			assertThatThrownBy(() -> memberService.createMember(request("Sporcu@Example.com")))
					.isInstanceOf(ConflictException.class)
					.hasMessageContaining("sporcu@example.com");

			verify(memberRepository, never()).save(any());
			verify(verificationCodeService, never()).issueAndSend(any(), any());
		}
	}

	@Nested
	class CompletingRegistration {

		@Test
		void shouldActivateMemberWhenTheCodeMatches() {
			Member member = pendingMember();
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.REGISTRATION, "123456"))
					.thenReturn(VerificationOutcome.OK);

			var response = memberService.completeRegistration(member.getId(), "123456");

			assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
			assertThat(response.verifiedAt()).isNotNull();
			assertThat(member.isActive()).isTrue();
		}

		@Test
		void shouldLeaveMemberPendingWhenTheCodeIsWrong() {
			Member member = pendingMember();
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
			when(verificationCodeService.tryConsume(member.getId(), VerificationPurpose.REGISTRATION, "000000"))
					.thenReturn(VerificationOutcome.MISMATCH);

			assertThatThrownBy(() -> memberService.completeRegistration(member.getId(), "000000"))
					.isInstanceOf(BadRequestException.class);

			assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING_VERIFICATION);
		}

		@Test
		void shouldRejectVerifyingAnAlreadyActiveMember() {
			Member member = pendingMember();
			member.activate(Instant.now());
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			assertThatThrownBy(() -> memberService.completeRegistration(member.getId(), "123456"))
					.isInstanceOf(ConflictException.class)
					.hasMessageContaining("zaten onaylanmış");

			verify(verificationCodeService, never()).tryConsume(any(), any(), any());
		}

		@Test
		void shouldRejectResendingToAnActiveMember() {
			Member member = pendingMember();
			member.activate(Instant.now());
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			assertThatThrownBy(() -> memberService.resendRegistrationCode(member.getId()))
					.isInstanceOf(ConflictException.class);

			verify(verificationCodeService, never()).issueAndSend(any(), any());
		}

		@Test
		void shouldResendToAPendingMember() {
			Member member = pendingMember();
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			memberService.resendRegistrationCode(member.getId());

			verify(verificationCodeService).issueAndSend(member, VerificationPurpose.REGISTRATION);
		}
	}

	@Nested
	class Updating {

		@Test
		void shouldTrimEditableFieldsAndLeaveEmailAlone() {
			Member member = pendingMember();
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			var response = memberService.updateMember(member.getId(), new UpdateMemberRequest(
					"  Mehmet Demir ", " +905559998877 ", 175, new BigDecimal("79.00"), "  Yüzme  "));

			assertThat(response.fullName()).isEqualTo("Mehmet Demir");
			assertThat(response.phone()).isEqualTo("+905559998877");
			assertThat(response.heightCm()).isEqualTo(175);
			assertThat(response.sportsBackground()).isEqualTo("Yüzme");
			assertThat(response.email()).isEqualTo("sporcu@example.com");
		}

		@Test
		void shouldFailForAnUnknownMember() {
			UUID unknown = UUID.randomUUID();
			when(memberRepository.findById(unknown)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> memberService.updateMember(unknown, new UpdateMemberRequest(
					"Ad Soyad", "+905551112233", 180, new BigDecimal("80.00"), "Fitness")))
					.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	class Deleting {

		@Test
		void shouldRemoveSessionsAndCodesBeforeTheMember() {
			Member member = pendingMember();
			when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

			memberService.deleteMember(member.getId());

			InOrder inOrder = Mockito.inOrder(sessionRepository, codeRepository, coachNoteRepository,
					measurementRepository, workoutLogRepository, trainingPlanRepository,
					nutritionPlanRepository, messageRepository, memberRepository);
			inOrder.verify(sessionRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(codeRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(coachNoteRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(measurementRepository).deleteAllByMemberId(member.getId());
			// Logs reference plan days, so they must be removed before the plans.
			inOrder.verify(workoutLogRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(trainingPlanRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(nutritionPlanRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(messageRepository).deleteAllByMemberId(member.getId());
			inOrder.verify(memberRepository).delete(member);
		}

		@Test
		void shouldFailForAnUnknownMember() {
			UUID unknown = UUID.randomUUID();
			when(memberRepository.findById(unknown)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> memberService.deleteMember(unknown))
					.isInstanceOf(NotFoundException.class)
					.hasMessageContaining("Üye bulunamadı");
		}
	}
}
