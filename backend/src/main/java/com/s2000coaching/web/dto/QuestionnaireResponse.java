package com.s2000coaching.web.dto;

import com.s2000coaching.domain.MemberQuestionnaire;
import com.s2000coaching.domain.QuestionnaireGoal;
import com.s2000coaching.domain.TrainingEquipment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The questions are sent with the answers so the wording lives in one place. {@code answers} is
 * null until the member has filled the questionnaire in.
 */
public record QuestionnaireResponse(List<String> questions, Answers answers) {

	public record Answers(List<Boolean> parq, boolean needsMedicalClearance, QuestionnaireGoal goal,
			int trainingDaysPerWeek, TrainingEquipment equipment, String injuries, Instant completedAt) {
	}

	public static QuestionnaireResponse of(List<String> questions, MemberQuestionnaire questionnaire) {
		if (questionnaire == null) {
			return new QuestionnaireResponse(questions, null);
		}
		List<Boolean> parq = new ArrayList<>();
		for (char c : questionnaire.getParqAnswers().toCharArray()) {
			parq.add(c == 'Y');
		}
		return new QuestionnaireResponse(questions, new Answers(parq, questionnaire.needsMedicalClearance(),
				questionnaire.getGoal(), questionnaire.getTrainingDaysPerWeek(), questionnaire.getEquipment(),
				questionnaire.getInjuries(), questionnaire.getCompletedAt()));
	}
}
