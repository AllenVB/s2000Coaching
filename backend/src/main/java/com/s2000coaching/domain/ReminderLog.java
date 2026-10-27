package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A reminder that was really sent. (member, kind, occasion) is unique, so it is never sent twice. */
@Entity
@Table(name = "reminder_log")
public class ReminderLog {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 40)
	private String kind;

	@Column(nullable = false, length = 60)
	private String occasion;

	@Column(name = "sent_at", nullable = false)
	private Instant sentAt;

	protected ReminderLog() {
	}

	public ReminderLog(UUID id, Member member, String kind, String occasion, Instant sentAt) {
		this.id = id;
		this.member = member;
		this.kind = kind;
		this.occasion = occasion;
		this.sentAt = sentAt;
	}

	public UUID getId() {
		return id;
	}

	public String getKind() {
		return kind;
	}

	public String getOccasion() {
		return occasion;
	}
}
