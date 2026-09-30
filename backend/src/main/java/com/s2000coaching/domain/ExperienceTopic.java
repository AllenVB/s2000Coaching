package com.s2000coaching.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "experience_topic")
public class ExperienceTopic {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "category_id", nullable = false)
	private ExperienceCategory category;

	@Column(nullable = false)
	private String name;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt asc")
	private List<ExperienceTopicNote> notes = new ArrayList<>();

	protected ExperienceTopic() {
	}

	public ExperienceTopic(UUID id, ExperienceCategory category, String name, Instant createdAt) {
		this.id = id;
		this.category = category;
		this.name = name;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<ExperienceTopicNote> getNotes() {
		return notes;
	}
}
