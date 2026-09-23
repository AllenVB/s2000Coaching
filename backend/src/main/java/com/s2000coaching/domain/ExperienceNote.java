package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "experience_note")
public class ExperienceNote {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true)
	private ExperienceCategory category;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExperienceNote() {
	}

	public ExperienceNote(UUID id, ExperienceCategory category, String content, Instant updatedAt) {
		this.id = id;
		this.category = category;
		this.content = content;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public ExperienceCategory getCategory() {
		return category;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
