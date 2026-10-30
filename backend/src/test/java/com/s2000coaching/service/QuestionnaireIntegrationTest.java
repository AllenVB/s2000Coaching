package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.QuestionnaireGoal;
import com.s2000coaching.domain.TrainingEquipment;
import com.s2000coaching.repository.MemberQuestionnaireRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.web.dto.UpsertQuestionnaireRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Against the real schema, because the mocked service test cannot see a NOT NULL column: the first
 * save of a questionnaire used to insert the row before its answers were filled in and failed with a
 * 500, which no test with a mocked repository could notice.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuestionnaireIntegrationTest {

	@Autowired
	private QuestionnaireService questionnaireService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private MemberQuestionnaireRepository questionnaireRepository;

	private Member newConsentedMember() {
		Member member = new Member(UUID.randomUUID(), UUID.randomUUID() + "@example.com", "Test Üye", "", 180,
				new BigDecimal("80.00"), "yok", MemberStatus.ACTIVE, Instant.now());
		member.assignMemberNumber(ThreadLocalRandom.current().nextInt(50_000, 99_999));
		member.grantHealthDataConsent(Instant.now());
		return memberRepository.save(member);
	}

	private static UpsertQuestionnaireRequest request(QuestionnaireGoal goal, int days, boolean firstYes) {
		return new UpsertQuestionnaireRequest(
				List.of(firstYes, false, false, false, false, false, false), goal, days, TrainingEquipment.SALON, " diz ");
	}

	@Test
	void shouldSaveTheFirstQuestionnaire() {
		Member member = newConsentedMember();

		var response = questionnaireService.save(member, request(QuestionnaireGoal.KAS_KAZANMA, 4, true));

		assertThat(response.answers().goal()).isEqualTo(QuestionnaireGoal.KAS_KAZANMA);
		var stored = questionnaireRepository.findByMemberId(member.getId()).orElseThrow();
		assertThat(stored.getParqAnswers()).isEqualTo("YNNNNNN");
		assertThat(stored.getTrainingDaysPerWeek()).isEqualTo(4);
		assertThat(stored.getInjuries()).isEqualTo("diz");
	}

	@Test
	void shouldReplaceItWhenFilledInAgain() {
		Member member = newConsentedMember();
		questionnaireService.save(member, request(QuestionnaireGoal.KAS_KAZANMA, 4, true));

		questionnaireService.save(member, request(QuestionnaireGoal.YAG_YAKMA, 3, false));

		assertThat(questionnaireRepository.findAll().stream().filter(q -> q.getMember().getId().equals(member.getId())))
				.hasSize(1)
				.allSatisfy(q -> {
					assertThat(q.getGoal()).isEqualTo(QuestionnaireGoal.YAG_YAKMA);
					assertThat(q.getParqAnswers()).isEqualTo("NNNNNNN");
				});
	}
}
