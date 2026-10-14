package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What was eaten on one day. This is the piece the whole analysis stands on: without daily
 * intake there is no maintenance calorie estimate and no calorie-to-weight relationship,
 * only a weight chart.
 *
 * <p>Macros and sodium are optional — a log may track calories alone, and half an answer is
 * better than refusing the import.
 */
@Entity
@Table(name = "case_study_intake")
public class CaseStudyIntake {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "case_study_id", nullable = false)
	private CaseStudy caseStudy;

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

	protected CaseStudyIntake() {
	}

	public CaseStudyIntake(UUID id, CaseStudy caseStudy, LocalDate consumedOn, int calories) {
		this.id = id;
		this.caseStudy = caseStudy;
		this.consumedOn = consumedOn;
		this.calories = calories;
	}

	public UUID getId() {
		return id;
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

	public Integer getSodiumMg() {
		return sodiumMg;
	}

	public void setSodiumMg(Integer sodiumMg) {
		this.sodiumMg = sodiumMg;
	}
}
