package com.s2000coaching.web.dto;

import com.s2000coaching.domain.QuestionnaireGoal;
import com.s2000coaching.domain.TrainingEquipment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** {@code parq} holds one answer per question, in the order the questions are listed. */
public record UpsertQuestionnaireRequest(
		@NotNull @Size(min = 7, max = 7) List<@NotNull Boolean> parq,
		@NotNull QuestionnaireGoal goal,
		@NotNull @Min(1) @Max(7) Integer trainingDaysPerWeek,
		@NotNull TrainingEquipment equipment,
		@Size(max = 2000) String injuries) {
}
