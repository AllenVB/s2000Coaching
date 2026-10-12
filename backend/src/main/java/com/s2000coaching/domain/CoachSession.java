package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Bearer session for the management area, mirroring {@link MemberSession}: only the token's
 * SHA-256 hash is stored and the raw token is returned once.
 */
@Entity
@Table(name = "coach_session")
public class CoachSession {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "coach_id", nullable = false)
	private CoachAccount coach;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected CoachSession() {
	}

	public CoachSession(UUID id, CoachAccount coach, String tokenHash, Instant createdAt, Instant expiresAt) {
		this.id = id;
		this.coach = coach;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public CoachAccount getCoach() {
		return coach;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isExpiredAt(Instant now) {
		return expiresAt.isBefore(now);
	}
}
