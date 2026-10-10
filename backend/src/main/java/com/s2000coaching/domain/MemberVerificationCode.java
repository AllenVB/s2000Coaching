package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A single-use numeric code mailed to a member. Only the SHA-256 hash of the code is
 * persisted, so a database dump never reveals a usable code.
 */
@Entity
@Table(name = "member_verification_code")
public class MemberVerificationCode {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationPurpose purpose;

	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	protected MemberVerificationCode() {
	}

	public MemberVerificationCode(UUID id, Member member, VerificationPurpose purpose, String codeHash,
			Instant createdAt, Instant expiresAt) {
		this.id = id;
		this.member = member;
		this.purpose = purpose;
		this.codeHash = codeHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public VerificationPurpose getPurpose() {
		return purpose;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void registerFailedAttempt() {
		this.attemptCount++;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public void consume(Instant consumedAt) {
		this.consumedAt = consumedAt;
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}

	public boolean isExpiredAt(Instant now) {
		return expiresAt.isBefore(now);
	}
}
