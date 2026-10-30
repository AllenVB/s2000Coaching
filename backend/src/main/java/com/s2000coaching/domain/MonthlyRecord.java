package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One month's body figures for a member. {@code period} is "YYYY-MM" and is fixed once the row
 * exists; the figures are all optional, so a month can be filled in over several visits.
 */
@Entity
@Table(name = "monthly_record")
public class MonthlyRecord {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 7, updatable = false)
	private String period;

	@Column(name = "height_cm", precision = 4, scale = 1)
	private BigDecimal heightCm;

	@Column(name = "weight_kg", precision = 5, scale = 1)
	private BigDecimal weightKg;

	@Column(name = "shoulder_cm", precision = 4, scale = 1)
	private BigDecimal shoulderCm;

	@Column(name = "chest_cm", precision = 4, scale = 1)
	private BigDecimal chestCm;

	@Column(name = "arm_cm", precision = 4, scale = 1)
	private BigDecimal armCm;

	@Column(name = "neck_cm", precision = 4, scale = 1)
	private BigDecimal neckCm;

	@Column(name = "waist_cm", precision = 4, scale = 1)
	private BigDecimal waistCm;

	@Column(name = "hip_cm", precision = 4, scale = 1)
	private BigDecimal hipCm;

	@Column(name = "thigh_cm", precision = 4, scale = 1)
	private BigDecimal thighCm;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MonthlyRecord() {
	}

	public MonthlyRecord(UUID id, Member member, String period, Instant now) {
		this.id = id;
		this.member = member;
		this.period = period;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() { return id; }
	public Member getMember() { return member; }
	public String getPeriod() { return period; }
	public BigDecimal getHeightCm() { return heightCm; }
	public BigDecimal getWeightKg() { return weightKg; }
	public BigDecimal getShoulderCm() { return shoulderCm; }
	public BigDecimal getChestCm() { return chestCm; }
	public BigDecimal getArmCm() { return armCm; }
	public BigDecimal getNeckCm() { return neckCm; }
	public BigDecimal getWaistCm() { return waistCm; }
	public BigDecimal getHipCm() { return hipCm; }
	public BigDecimal getThighCm() { return thighCm; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	/** Replaces every figure at once: the form always sends the whole table. */
	public void setFigures(BigDecimal heightCm, BigDecimal weightKg, BigDecimal shoulderCm, BigDecimal chestCm,
			BigDecimal armCm, BigDecimal neckCm, BigDecimal waistCm, BigDecimal hipCm, BigDecimal thighCm,
			Instant now) {
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.shoulderCm = shoulderCm;
		this.chestCm = chestCm;
		this.armCm = armCm;
		this.neckCm = neckCm;
		this.waistCm = waistCm;
		this.hipCm = hipCm;
		this.thighCm = thighCm;
		this.updatedAt = now;
	}
}
