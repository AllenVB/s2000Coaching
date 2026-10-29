package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * One exercise as prescribed inside a plan day. {@code reps} is free text ("8-10", "AMRAP")
 * because that is how programmes are actually written.
 */
@Entity
@Table(name = "training_plan_exercise")
public class TrainingPlanExercise {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "day_id", nullable = false)
	private TrainingPlanDay day;

	@ManyToOne
	@JoinColumn(name = "exercise_id", nullable = false)
	private Exercise exercise;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "set_count", nullable = false)
	private int setCount;

	@Column(nullable = false, length = 80)
	private String reps;

	/** Rep target per set, "30|25|20"; null for exercises written with a single free-text reps. */
	@Column(name = "set_reps", length = 80)
	private String setReps;

	@Column(name = "target_weight_kg", precision = 6, scale = 2)
	private BigDecimal targetWeightKg;

	@Column(name = "rest_seconds")
	private Integer restSeconds;

	@Column(columnDefinition = "text")
	private String note;

	protected TrainingPlanExercise() {
	}

	public TrainingPlanExercise(UUID id, TrainingPlanDay day, Exercise exercise, int displayOrder, int setCount,
			String reps, BigDecimal targetWeightKg, Integer restSeconds, String note) {
		this.id = id;
		this.day = day;
		this.exercise = exercise;
		this.displayOrder = displayOrder;
		this.setCount = setCount;
		this.reps = reps;
		this.targetWeightKg = targetWeightKg;
		this.restSeconds = restSeconds;
		this.note = note;
	}

	public UUID getId() {
		return id;
	}

	public TrainingPlanDay getDay() {
		return day;
	}

	public Exercise getExercise() {
		return exercise;
	}

	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public int getSetCount() {
		return setCount;
	}

	public void setSetCount(int setCount) {
		this.setCount = setCount;
	}

	public String getReps() {
		return reps;
	}

	public void setReps(String reps) {
		this.reps = reps;
	}

	public String getSetReps() {
		return setReps;
	}

	public void setSetReps(String setReps) {
		this.setReps = setReps;
	}

	public BigDecimal getTargetWeightKg() {
		return targetWeightKg;
	}

	public void setTargetWeightKg(BigDecimal targetWeightKg) {
		this.targetWeightKg = targetWeightKg;
	}

	public Integer getRestSeconds() {
		return restSeconds;
	}

	public void setRestSeconds(Integer restSeconds) {
		this.restSeconds = restSeconds;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
