package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** One set the member recorded against a prescribed exercise. */
@Entity
@Table(name = "set_log")
public class SetLog {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "workout_log_id", nullable = false)
	private WorkoutLog workoutLog;

	@ManyToOne
	@JoinColumn(name = "plan_exercise_id", nullable = false)
	private TrainingPlanExercise planExercise;

	@Column(name = "set_number", nullable = false)
	private int setNumber;

	@Column(nullable = false)
	private int reps;

	@Column(name = "weight_kg", precision = 6, scale = 2)
	private BigDecimal weightKg;

	/** Perceived effort, 1 to 10; null when the member did not say. */
	@Column(precision = 3, scale = 1)
	private BigDecimal rpe;

	protected SetLog() {
	}

	public SetLog(UUID id, WorkoutLog workoutLog, TrainingPlanExercise planExercise, int setNumber, int reps,
			BigDecimal weightKg) {
		this.id = id;
		this.workoutLog = workoutLog;
		this.planExercise = planExercise;
		this.setNumber = setNumber;
		this.reps = reps;
		this.weightKg = weightKg;
	}

	public BigDecimal getRpe() {
		return rpe;
	}

	public void setRpe(BigDecimal rpe) {
		this.rpe = rpe;
	}

	public UUID getId() {
		return id;
	}

	public WorkoutLog getWorkoutLog() {
		return workoutLog;
	}

	public TrainingPlanExercise getPlanExercise() {
		return planExercise;
	}

	public int getSetNumber() {
		return setNumber;
	}

	public int getReps() {
		return reps;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}
}
