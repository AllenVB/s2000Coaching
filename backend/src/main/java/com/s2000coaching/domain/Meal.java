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

/** One meal in a nutrition plan ("Kahvaltı"), ordered through the day. */
@Entity
@Table(name = "meal")
public class Meal {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "plan_id", nullable = false)
	private NutritionPlan plan;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private String name;

	/** Free text like "08:00" or "antrenman sonrası" — not a real time, on purpose. */
	@Column(name = "time_hint", length = 60)
	private String timeHint;

	@Column(columnDefinition = "text")
	private String note;

	@OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder asc")
	private List<MealItem> items = new ArrayList<>();

	protected Meal() {
	}

	public Meal(UUID id, NutritionPlan plan, int displayOrder, String name, String timeHint, String note) {
		this.id = id;
		this.plan = plan;
		this.displayOrder = displayOrder;
		this.name = name;
		this.timeHint = timeHint;
		this.note = note;
	}

	public UUID getId() {
		return id;
	}

	public NutritionPlan getPlan() {
		return plan;
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

	public String getTimeHint() {
		return timeHint;
	}

	public void setTimeHint(String timeHint) {
		this.timeHint = timeHint;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public List<MealItem> getItems() {
		return items;
	}
}
