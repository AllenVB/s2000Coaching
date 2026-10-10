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

@Entity
@Table(name = "member")
public class Member {

	@Id
	private UUID id;

	/** Always stored lower-cased and trimmed — see {@code MemberService.normalizeEmail}. */
	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false)
	private String phone;

	@Column(name = "height_cm", nullable = false)
	private int heightCm;

	@Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "sports_background", nullable = false, columnDefinition = "text")
	private String sportsBackground;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MemberStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	/**
	 * When the member accepted that their health data (measurements and the notes attached to
	 * them) may be processed. KVKK treats this as special-category data, so nothing is written
	 * until this is set.
	 */
	@Column(name = "health_data_consent_at")
	private Instant healthDataConsentAt;

	protected Member() {
	}

	public Member(UUID id, String email, String fullName, String phone, int heightCm, BigDecimal weightKg,
			String sportsBackground, MemberStatus status, Instant createdAt) {
		this.id = id;
		this.email = email;
		this.fullName = fullName;
		this.phone = phone;
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.sportsBackground = sportsBackground;
		this.status = status;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public int getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(int heightCm) {
		this.heightCm = heightCm;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(BigDecimal weightKg) {
		this.weightKg = weightKg;
	}

	public String getSportsBackground() {
		return sportsBackground;
	}

	public void setSportsBackground(String sportsBackground) {
		this.sportsBackground = sportsBackground;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public boolean isActive() {
		return status == MemberStatus.ACTIVE;
	}

	public void activate(Instant verifiedAt) {
		this.status = MemberStatus.ACTIVE;
		this.verifiedAt = verifiedAt;
	}

	public Instant getHealthDataConsentAt() {
		return healthDataConsentAt;
	}

	public boolean hasHealthDataConsent() {
		return healthDataConsentAt != null;
	}

	/** Idempotent: re-accepting keeps the original timestamp, which is the one that counts. */
	public void grantHealthDataConsent(Instant grantedAt) {
		if (this.healthDataConsentAt == null) {
			this.healthDataConsentAt = grantedAt;
		}
	}
}
