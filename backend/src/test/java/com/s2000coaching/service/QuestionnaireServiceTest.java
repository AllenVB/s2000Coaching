package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberQuestionnaire;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.QuestionnaireGoal;
import com.s2000coaching.domain.TrainingEquipment;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.MemberQuestionnaireRepository;
import com.s2000coaching.web.dto.UpsertQuestionnaireRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionnaireServiceTest {

	@Mock
	private MemberQuestionnaireRepository repository;

	@Mock
	private MemberService memberService;

	private QuestionnaireService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new QuestionnaireService(repository, memberService,
				Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC));
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	private static UpsertQuestionnaireRequest request(boolean... parq) {
		var answers = new java.util.ArrayList<Boolean>();
		for (boolean answer : parq) {
			answers.add(answer);
		}
		return new UpsertQuestionnaireRequest(answers, QuestionnaireGoal.KAS_KAZANMA, 4, TrainingEquipment.SALON,
				"  sağ diz  ");
	}

	private static final boolean[] ALL_NO = {false, false, false, false, false, false, false};

	@Test
	void shouldRefuseWithoutHealthDataConsent() {
		assertThatThrownBy(() -> service.save(member, request(ALL_NO))).isInstanceOf(BadRequestException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void shouldStoreAnswersInQuestionOrderAndFlagAnyYes() {
		member.grantHealthDataConsent(Instant.now());
		when(repository.findByMemberId(member.getId())).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

		var response = service.save(member, request(false, false, false, false, true, false, false));

		assertThat(response.answers().parq()).containsExactly(false, false, false, false, true, false, false);
		assertThat(response.answers().needsMedicalClearance()).isTrue();
		assertThat(response.answers().injuries()).isEqualTo("sağ diz");
		assertThat(response.questions()).hasSize(7);
	}

	@Test
	void shouldNotFlagWhenEveryAnswerIsNo() {
		member.grantHealthDataConsent(Instant.now());
		when(repository.findByMemberId(member.getId())).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

		assertThat(service.save(member, request(ALL_NO)).answers().needsMedicalClearance()).isFalse();
	}

	@Test
	void shouldRefuseAnIncompleteSetOfAnswers() {
		member.grantHealthDataConsent(Instant.now());

		assertThatThrownBy(() -> service.save(member, request(false, false, false)))
				.isInstanceOf(BadRequestException.class);
	}

	/** Filling it in again replaces the earlier answers rather than adding a second row. */
	@Test
	void shouldReplaceAnEarlierQuestionnaire() {
		member.grantHealthDataConsent(Instant.now());
		var existing = new MemberQuestionnaire(UUID.randomUUID(), member, Instant.now());
		existing.fill("YYYYYYY", QuestionnaireGoal.YAG_YAKMA, 3, TrainingEquipment.EVDE, null, Instant.now());
		when(repository.findByMemberId(member.getId())).thenReturn(Optional.of(existing));

		var response = service.save(member, request(ALL_NO));

		assertThat(response.answers().needsMedicalClearance()).isFalse();
		assertThat(response.answers().goal()).isEqualTo(QuestionnaireGoal.KAS_KAZANMA);
		verify(repository, never()).save(any());
	}

	@Test
	void shouldReturnTheQuestionsEvenBeforeAnythingIsAnswered() {
		when(repository.findByMemberId(member.getId())).thenReturn(Optional.empty());

		var response = service.get(member.getId());

		assertThat(response.answers()).isNull();
		assertThat(response.questions()).isEqualTo(QuestionnaireService.QUESTIONS);
	}

	@Test
	void shouldEraseEverythingOfAMember() {
		service.eraseAllOf(member.getId());

		verify(repository).deleteAllByMemberId(member.getId());
	}
}
