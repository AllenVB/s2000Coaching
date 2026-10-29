package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A member's starting questionnaire: the PAR-Q readiness questions and what they want from
 * training. Health data under KVKK: written only after the member's consent, read only by the
 * member and their coach.
 */
@Entity
@Table(name = "member_questionnaire")
public class MemberQuestionnaire {

	@Id
	private UUID id;

	@OneToOne
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	/** One character per PAR-Q question, in order: Y for yes, N for no. */
	@Column(name = "parq_answers", nullable = false, length = 7)
	private String parqAnswers;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private QuestionnaireGoal goal;

	@Column(name = "training_days_per_week", nullable = false)
	private int trainingDaysPerWeek;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TrainingEquipment equipment;

	@Column(columnDefinition = "text")
	private String injuries;

	@Column(name = "completed_at", nullable = false)
	private Instant completedAt;

	protected MemberQuestionnaire() {
	}

	public MemberQuestionnaire(UUID id, Member member, Instant completedAt) {
		this.id = id;
		this.member = member;
		this.completedAt = completedAt;
	}

	public void fill(String parqAnswers, QuestionnaireGoal goal, int trainingDaysPerWeek, TrainingEquipment equipment,
			String injuries, Instant now) {
		this.parqAnswers = parqAnswers;
		this.goal = goal;
		this.trainingDaysPerWeek = trainingDaysPerWeek;
		this.equipment = equipment;
		this.injuries = injuries;
		this.completedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getParqAnswers() {
		return parqAnswers;
	}

	public QuestionnaireGoal getGoal() {
		return goal;
	}

	public int getTrainingDaysPerWeek() {
		return trainingDaysPerWeek;
	}

	public TrainingEquipment getEquipment() {
		return equipment;
	}

	public String getInjuries() {
		return injuries;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	/** True when any readiness question was answered yes. */
	public boolean needsMedicalClearance() {
		return parqAnswers != null && parqAnswers.indexOf('Y') >= 0;
	}
}
