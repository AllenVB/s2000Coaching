package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A member's steps and cardio minutes for one day. One row per member per day; either figure may be null. */
@Entity
@Table(name = "member_activity")
public class MemberActivity {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "performed_on", nullable = false)
	private LocalDate performedOn;

	private Integer steps;

	@Column(name = "cardio_minutes")
	private Integer cardioMinutes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MemberActivity() {
	}

	public MemberActivity(UUID id, Member member, LocalDate performedOn, Instant now) {
		this.id = id;
		this.member = member;
		this.performedOn = performedOn;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public LocalDate getPerformedOn() {
		return performedOn;
	}

	public Integer getSteps() {
		return steps;
	}

	public Integer getCardioMinutes() {
		return cardioMinutes;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void record(Integer steps, Integer cardioMinutes, Instant now) {
		this.steps = steps;
		this.cardioMinutes = cardioMinutes;
		this.updatedAt = now;
	}
}
