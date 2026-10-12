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
 * One message in a member's thread with their coach. {@code readAt} is set when the *other*
 * side opens the thread, which is what drives the unread badges.
 */
@Entity
@Table(name = "message")
public class Message {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MessageSender sender;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "read_at")
	private Instant readAt;

	protected Message() {
	}

	public Message(UUID id, Member member, MessageSender sender, String body, Instant createdAt) {
		this.id = id;
		this.member = member;
		this.sender = sender;
		this.body = body;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public MessageSender getSender() {
		return sender;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public boolean isUnread() {
		return readAt == null;
	}

	public void markRead(Instant readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}
}
