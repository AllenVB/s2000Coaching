package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One check-in by a member. At most one row per member per day — re-submitting the same date
 * overwrites it, which is what someone correcting a typo expects.
 *
 * <p>Health data under KVKK: only the member and their coach can read it, and the member
 * must have given explicit consent ({@link Member#getHealthDataConsentAt()}) before any row
 * is written.
 */
@Entity
@Table(name = "measurement")
public class Measurement {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "measured_on", nullable = false)
	private LocalDate measuredOn;

	@Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "waist_cm", precision = 5, scale = 1)
	private BigDecimal waistCm;

	@Column(name = "chest_cm", precision = 5, scale = 1)
	private BigDecimal chestCm;

	@Column(name = "hip_cm", precision = 5, scale = 1)
	private BigDecimal hipCm;

	@Column(name = "arm_cm", precision = 5, scale = 1)
	private BigDecimal armCm;

	@Column(name = "thigh_cm", precision = 5, scale = 1)
	private BigDecimal thighCm;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Measurement() {
	}

	public Measurement(UUID id, Member member, LocalDate measuredOn, BigDecimal weightKg, Instant createdAt) {
		this.id = id;
		this.member = member;
		this.measuredOn = measuredOn;
		this.weightKg = weightKg;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public LocalDate getMeasuredOn() {
		return measuredOn;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(BigDecimal weightKg) {
		this.weightKg = weightKg;
	}

	public BigDecimal getWaistCm() {
		return waistCm;
	}

	public void setWaistCm(BigDecimal waistCm) {
		this.waistCm = waistCm;
	}

	public BigDecimal getChestCm() {
		return chestCm;
	}

	public void setChestCm(BigDecimal chestCm) {
		this.chestCm = chestCm;
	}

	public BigDecimal getHipCm() {
		return hipCm;
	}

	public void setHipCm(BigDecimal hipCm) {
		this.hipCm = hipCm;
	}

	public BigDecimal getArmCm() {
		return armCm;
	}

	public void setArmCm(BigDecimal armCm) {
		this.armCm = armCm;
	}

	public BigDecimal getThighCm() {
		return thighCm;
	}

	public void setThighCm(BigDecimal thighCm) {
		this.thighCm = thighCm;
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
