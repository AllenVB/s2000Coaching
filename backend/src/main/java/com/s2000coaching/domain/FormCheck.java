package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A member's request for the coach to review their form, with the coach's answer. */
@Entity
@Table(name = "form_check")
public class FormCheck {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "exercise_name", nullable = false, length = 120)
	private String exerciseName;

	/** Canonical YouTube address; the player is built from its video id alone. */
	@Column(name = "video_url", nullable = false, length = 200)
	private String videoUrl;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "coach_reply", columnDefinition = "text")
	private String coachReply;

	@Column(name = "coach_replied_at")
	private Instant coachRepliedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected FormCheck() {
	}

	public FormCheck(UUID id, Member member, String exerciseName, String videoUrl, String note, Instant createdAt) {
		this.id = id;
		this.member = member;
		this.exerciseName = exerciseName;
		this.videoUrl = videoUrl;
		this.note = note;
		this.createdAt = createdAt;
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

	public String getExerciseName() {
		return exerciseName;
	}

	public String getVideoUrl() {
		return videoUrl;
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

	public Instant getCreatedAt() {
		return createdAt;
	}
}
