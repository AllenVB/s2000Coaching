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
 * A nutrition programme, mirroring {@link TrainingPlan}: {@code member == null} makes it a
 * template, assigning one deep-copies it, and only one plan per member is active.
 *
 * <p>The macro targets are the plan's prescription; what the meals add up to is computed from
 * the items, so the coach can see the gap between the two.
 */
@Entity
@Table(name = "nutrition_plan")
public class NutritionPlan {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "text")
	private String note;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "target_calories")
	private Integer targetCalories;

	@Column(name = "target_protein_g")
	private Integer targetProteinG;

	@Column(name = "target_carbs_g")
	private Integer targetCarbsG;

	@Column(name = "target_fat_g")
	private Integer targetFatG;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder asc")
	private List<Meal> meals = new ArrayList<>();

	protected NutritionPlan() {
	}

	public NutritionPlan(UUID id, Member member, String name, String note, boolean active,
			Instant createdAt) {
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

	public Integer getTargetCalories() {
		return targetCalories;
	}

	public void setTargetCalories(Integer targetCalories) {
		this.targetCalories = targetCalories;
	}

	public Integer getTargetProteinG() {
		return targetProteinG;
	}

	public void setTargetProteinG(Integer targetProteinG) {
		this.targetProteinG = targetProteinG;
	}

	public Integer getTargetCarbsG() {
		return targetCarbsG;
	}

	public void setTargetCarbsG(Integer targetCarbsG) {
		this.targetCarbsG = targetCarbsG;
	}

	public Integer getTargetFatG() {
		return targetFatG;
	}

	public void setTargetFatG(Integer targetFatG) {
		this.targetFatG = targetFatG;
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

	public List<Meal> getMeals() {
		return meals;
	}
}
