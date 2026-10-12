package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A person who can use the management area. Replaces the single shared {@code ADMIN_TOKEN}:
 * accounts can be told apart, their passwords are hashed, and a stolen session can be
 * revoked without changing a secret everyone shares.
 */
@Entity
@Table(name = "coach_account")
public class CoachAccount {

	/** Wrong guesses before the account stops accepting passwords for a while. */
	public static final int MAX_FAILED_ATTEMPTS = 5;

	@Id
	private UUID id;

	/** Stored lower-cased and trimmed, like a member's. */
	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected CoachAccount() {
	}

	public CoachAccount(UUID id, String email, String passwordHash, String fullName, Instant createdAt) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isLockedAt(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	/** Counts a wrong password and locks the account once the limit is reached. */
	public void registerFailedAttempt(Instant now, java.time.Duration lockFor) {
		this.failedAttempts++;
		if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
			this.lockedUntil = now.plus(lockFor);
			this.failedAttempts = 0;
		}
	}

	public void registerSuccessfulLogin(Instant now) {
		this.failedAttempts = 0;
		this.lockedUntil = null;
		this.lastLoginAt = now;
	}
}
