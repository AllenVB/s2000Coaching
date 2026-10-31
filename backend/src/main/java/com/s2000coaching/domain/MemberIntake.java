package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A member's food intake for one day, as totals. One row per member per day.
 *
 * <p>Health data under KVKK, like {@link Measurement}: only the member and their coach read it,
 * and nothing is written before the member's health-data consent.
 */
@Entity
@Table(name = "member_intake")
public class MemberIntake {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "consumed_on", nullable = false)
	private LocalDate consumedOn;

	@Column(nullable = false)
	private int calories;

	@Column(name = "protein_g")
	private Integer proteinG;

	@Column(name = "carbs_g")
	private Integer carbsG;

	@Column(name = "fat_g")
	private Integer fatG;

	@Column(name = "sodium_mg")
	private Integer sodiumMg;

	/** Litres of water drunk that day; null when not written down. */
	@Column(name = "water_liters", precision = 3, scale = 1)
	private java.math.BigDecimal waterLiters;

	/** Training or rest day; null when the member did not say. */
	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(name = "day_type", length = 12)
	private IntakeDayType dayType;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MemberIntake() {
	}

	public MemberIntake(UUID id, Member member, LocalDate consumedOn, int calories, Instant now) {
		this.id = id;
		this.member = member;
		this.consumedOn = consumedOn;
		this.calories = calories;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public LocalDate getConsumedOn() {
		return consumedOn;
	}

	public int getCalories() {
		return calories;
	}

	public void setCalories(int calories) {
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

	public java.math.BigDecimal getWaterLiters() {
		return waterLiters;
	}

	public void setWaterLiters(java.math.BigDecimal waterLiters) {
		this.waterLiters = waterLiters;
	}

	public IntakeDayType getDayType() {
		return dayType;
	}

	public void setDayType(IntakeDayType dayType) {
		this.dayType = dayType;
	}

	public Integer getSodiumMg() {
		return sodiumMg;
	}

	public void setSodiumMg(Integer sodiumMg) {
		this.sodiumMg = sodiumMg;
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
}
