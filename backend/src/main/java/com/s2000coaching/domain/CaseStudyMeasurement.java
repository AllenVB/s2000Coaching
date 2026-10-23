package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One scale reading in a case study. Only the date and weight are required: a CSV export may
 * carry nothing else, and the analysis degrades to what it actually has rather than refusing
 * the import.
 *
 * <p>{@code neckCm} is here and not on {@link Measurement} because the Navy body-fat formula
 * needs it; without it the analysis falls back to another formula or omits the estimate.
 */
@Entity
@Table(name = "case_study_measurement")
public class CaseStudyMeasurement {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "case_study_id", nullable = false)
	private CaseStudy caseStudy;

	@Column(name = "measured_on", nullable = false)
	private LocalDate measuredOn;

	@Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "neck_cm", precision = 5, scale = 1)
	private BigDecimal neckCm;

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

	@Column(name = "shoulder_cm", precision = 5, scale = 1)
	private BigDecimal shoulderCm;

	/** What a scale or a caliper actually read, as opposed to what the formulas estimate. */
	@Column(name = "body_fat_percent", precision = 4, scale = 1)
	private BigDecimal bodyFatPercent;

	protected CaseStudyMeasurement() {
	}

	public CaseStudyMeasurement(UUID id, CaseStudy caseStudy, LocalDate measuredOn, BigDecimal weightKg) {
		this.id = id;
		this.caseStudy = caseStudy;
		this.measuredOn = measuredOn;
		this.weightKg = weightKg;
	}

	public UUID getId() {
		return id;
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

	public BigDecimal getNeckCm() {
		return neckCm;
	}

	public void setNeckCm(BigDecimal neckCm) {
		this.neckCm = neckCm;
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

	public BigDecimal getShoulderCm() {
		return shoulderCm;
	}

	public void setShoulderCm(BigDecimal shoulderCm) {
		this.shoulderCm = shoulderCm;
	}

	public BigDecimal getBodyFatPercent() {
		return bodyFatPercent;
	}

	public void setBodyFatPercent(BigDecimal bodyFatPercent) {
		this.bodyFatPercent = bodyFatPercent;
	}
}
