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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A training programme. {@code member == null} makes it a template: written once, then copied
 * onto a member. Copying rather than referencing means editing a member's plan never disturbs
 * the template or anyone else's copy.
 */
@Entity
@Table(name = "training_plan")
public class TrainingPlan {

	@Id
	private UUID id;

	/** Null for a template; set for a plan belonging to one member. */
	@ManyToOne
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "text")
	private String note;

	/** Only one plan per member is active; that is the one the member area shows. */
	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("dayOrder asc")
	private List<TrainingPlanDay> days = new ArrayList<>();

	protected TrainingPlan() {
	}

	public TrainingPlan(UUID id, Member member, String name, String note, boolean active, Instant createdAt) {
		this.id = id;
		this.member = member;
		this.name = name;
		this.note = note;
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public boolean isTemplate() {
		return member == null;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
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

	public List<TrainingPlanDay> getDays() {
		return days;
	}
}
