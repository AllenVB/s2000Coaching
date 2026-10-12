package com.s2000coaching.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One session within a plan ("Gün 1 — İtiş"), ordered by {@code dayOrder}. */
@Entity
@Table(name = "training_plan_day")
public class TrainingPlanDay {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "plan_id", nullable = false)
	private TrainingPlan plan;

	@Column(name = "day_order", nullable = false)
	private int dayOrder;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "text")
	private String note;

	@OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder asc")
	private List<TrainingPlanExercise> exercises = new ArrayList<>();

	protected TrainingPlanDay() {
	}

	public TrainingPlanDay(UUID id, TrainingPlan plan, int dayOrder, String name, String note) {
		this.id = id;
		this.plan = plan;
		this.dayOrder = dayOrder;
		this.name = name;
		this.note = note;
	}

	public UUID getId() {
		return id;
	}

	public TrainingPlan getPlan() {
		return plan;
	}

	public int getDayOrder() {
		return dayOrder;
	}

	public void setDayOrder(int dayOrder) {
		this.dayOrder = dayOrder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public List<TrainingPlanExercise> getExercises() {
		return exercises;
	}
}
