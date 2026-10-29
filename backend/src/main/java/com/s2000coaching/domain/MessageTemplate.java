package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A reply the coach keeps around to reuse: the "hazır yanıt" chips above the composer.
 * Templates belong to the coach as a whole, not to a member — there is one coach.
 */
@Entity
@Table(name = "message_template")
public class MessageTemplate {

	@Id
	private UUID id;

	@Column(nullable = false, length = 80)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MessageTemplate() {
	}

	public MessageTemplate(UUID id, String title, String body, Instant createdAt) {
		this.id = id;
		this.title = title;
		this.body = body;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
