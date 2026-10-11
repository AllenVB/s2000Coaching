package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One food in a meal. {@code amount} is free text ("80 g", "1 orta boy") because that is how
 * plans are written; the macros are optional so a coach can prescribe food without counting.
 */
@Entity
@Table(name = "meal_item")
public class MealItem {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "meal_id", nullable = false)
	private Meal meal;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private String name;

	@Column(length = 60)
	private String amount;

	private Integer calories;

	@Column(name = "protein_g")
	private Integer proteinG;

	@Column(name = "carbs_g")
	private Integer carbsG;

	@Column(name = "fat_g")
	private Integer fatG;

	@Column(columnDefinition = "text")
	private String note;

	protected MealItem() {
	}

	public MealItem(UUID id, Meal meal, int displayOrder, String name, String amount, Integer calories,
			Integer proteinG, Integer carbsG, Integer fatG, String note) {
		this.id = id;
		this.meal = meal;
		this.displayOrder = displayOrder;
		this.name = name;
		this.amount = amount;
		this.calories = calories;
		this.proteinG = proteinG;
		this.carbsG = carbsG;
		this.fatG = fatG;
		this.note = note;
	}

	public UUID getId() {
		return id;
	}

	public Meal getMeal() {
		return meal;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public Integer getCalories() {
		return calories;
	}

	public void setCalories(Integer calories) {
		this.calories = calories;
	}

	public Integer getProteinG() {
		return proteinG;
	}

	public void setProteinG(Integer proteinG) {
		this.proteinG = proteinG;
	}

	public Integer getCarbsG() {
		return carbsG;
	}

	public void setCarbsG(Integer carbsG) {
		this.carbsG = carbsG;
	}

	public Integer getFatG() {
		return fatG;
	}

	public void setFatG(Integer fatG) {
		this.fatG = fatG;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
