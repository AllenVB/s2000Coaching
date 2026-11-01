package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A percentage discount code, optionally limited in number of uses and in time. */
@Entity
@Table(name = "coupon")
public class Coupon {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 40)
	private String code;

	@Column(name = "percent_off", nullable = false)
	private int percentOff;

	@Column(name = "max_uses")
	private Integer maxUses;

	@Column(name = "used_count", nullable = false)
	private int usedCount;

	@Column(name = "expires_on")
	private LocalDate expiresOn;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Coupon() {
	}

	public Coupon(UUID id, String code, int percentOff, Integer maxUses, LocalDate expiresOn, Instant now) {
		this.id = id;
		this.code = code;
		this.percentOff = percentOff;
		this.maxUses = maxUses;
		this.usedCount = 0;
		this.expiresOn = expiresOn;
		this.active = true;
		this.createdAt = now;
	}

	/** Usable today: switched on, not past its last day, and not used up. */
	public boolean isUsableOn(LocalDate today) {
		return active && (expiresOn == null || !today.isAfter(expiresOn)) && (maxUses == null || usedCount < maxUses);
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public int getPercentOff() {
		return percentOff;
	}

	public Integer getMaxUses() {
		return maxUses;
	}

	public int getUsedCount() {
		return usedCount;
	}

	public LocalDate getExpiresOn() {
		return expiresOn;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
