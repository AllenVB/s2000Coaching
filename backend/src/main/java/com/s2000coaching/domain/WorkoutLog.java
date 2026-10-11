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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What the member actually did on one plan day, on one date. At most one per
 * (member, day, date) so re-saving a session corrects it instead of duplicating it.
 */
@Entity
@Table(name = "workout_log")
public class WorkoutLog {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne
	@JoinColumn(name = "day_id", nullable = false)
	private TrainingPlanDay day;

	@Column(name = "performed_on", nullable = false)
	private LocalDate performedOn;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "workoutLog", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("setNumber asc")
	private List<SetLog> sets = new ArrayList<>();

	protected WorkoutLog() {
	}

	public WorkoutLog(UUID id, Member member, TrainingPlanDay day, LocalDate performedOn, Instant createdAt) {
		this.id = id;
		this.member = member;
		this.day = day;
		this.performedOn = performedOn;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public TrainingPlanDay getDay() {
		return day;
	}

	public LocalDate getPerformedOn() {
		return performedOn;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<SetLog> getSets() {
		return sets;
	}
}
