package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A free pre-assessment request from the public site. Neither a member nor an order. */
@Entity
@Table(name = "lead")
public class Lead {

	@Id
	private UUID id;

	@Column(name = "full_name", nullable = false, length = 160)
	private String fullName;

	@Column(nullable = false, length = 40)
	private String phone;

	@Column(length = 255)
	private String email;

	@Column(nullable = false, length = 40)
	private String goal;

	@Column(length = 40)
	private String level;

	@Column(name = "height_cm")
	private Integer heightCm;

	@Column(name = "weight_kg", precision = 5, scale = 1)
	private BigDecimal weightKg;

	@Column(columnDefinition = "text")
	private String note;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private LeadStatus status;

	@Column(name = "consent_at", nullable = false)
	private Instant consentAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Lead() {
	}

	public Lead(UUID id, String fullName, String phone, String email, String goal, String level, Integer heightCm,
			BigDecimal weightKg, String note, Instant now) {
		this.id = id;
		this.fullName = fullName;
		this.phone = phone;
		this.email = email;
		this.goal = goal;
		this.level = level;
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.note = note;
		this.status = LeadStatus.NEW;
		this.consentAt = now;
		this.createdAt = now;
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public String getGoal() {
		return goal;
	}

	public String getLevel() {
		return level;
	}

	public Integer getHeightCm() {
		return heightCm;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public String getNote() {
		return note;
	}

	public LeadStatus getStatus() {
		return status;
	}

	public void setStatus(LeadStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
