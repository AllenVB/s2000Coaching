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

/**
 * A member's weekly check-in, and the coach's answer to it. One per member per week,
 * identified by the Monday of that week.
 *
 * <p>Health data under KVKK like {@link Measurement}: nothing is written before the member's
 * health-data consent, and only the member and their coach read it.
 */
@Entity
@Table(name = "check_in")
public class CheckIn {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "week_start", nullable = false)
	private LocalDate weekStart;

	/** 1 (bad) to 5 (good). */
	@Column(nullable = false)
	private int sleep;

	/** 1 (calm) to 5 (very stressed). */
	@Column(nullable = false)
	private int stress;

	/** 1 (drained) to 5 (energetic). */
	@Column(nullable = false)
	private int energy;

	/** 1 (never hungry) to 5 (constantly hungry). */
	@Column(nullable = false)
	private int hunger;

	/** How much of the diet was followed, 0 to 100; null when the member skipped the question. */
	@Column(name = "diet_adherence_percent")
	private Integer dietAdherencePercent;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "coach_reply", columnDefinition = "text")
	private String coachReply;

	@Column(name = "coach_replied_at")
	private Instant coachRepliedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CheckIn() {
	}

	public CheckIn(UUID id, Member member, LocalDate weekStart, Instant now) {
		this.id = id;
		this.member = member;
		this.weekStart = weekStart;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void fill(int sleep, int stress, int energy, int hunger, Integer dietAdherencePercent, String note,
			Instant now) {
		this.sleep = sleep;
		this.stress = stress;
		this.energy = energy;
		this.hunger = hunger;
		this.dietAdherencePercent = dietAdherencePercent;
		this.note = note;
		this.updatedAt = now;
		// Changed answers are a new thing for the coach to read, so any earlier reply no longer
		// answers them and the check-in goes back to "waiting".
		this.coachReply = null;
		this.coachRepliedAt = null;
	}

	public void reply(String reply, Instant now) {
		this.coachReply = reply;
		this.coachRepliedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public LocalDate getWeekStart() {
		return weekStart;
	}

	public int getSleep() {
		return sleep;
	}

	public int getStress() {
		return stress;
	}

	public int getEnergy() {
		return energy;
	}

	public int getHunger() {
		return hunger;
	}

	public Integer getDietAdherencePercent() {
		return dietAdherencePercent;
	}

	public String getNote() {
		return note;
	}

	public String getCoachReply() {
		return coachReply;
	}

	public Instant getCoachRepliedAt() {
		return coachRepliedAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
