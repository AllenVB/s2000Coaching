package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "experience_topic_note")
public class ExperienceTopicNote {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "topic_id", nullable = false)
	private ExperienceTopic topic;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ExperienceTopicNote() {
	}

	public ExperienceTopicNote(UUID id, ExperienceTopic topic, String content, Instant createdAt) {
		this.id = id;
		this.topic = topic;
		this.content = content;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
