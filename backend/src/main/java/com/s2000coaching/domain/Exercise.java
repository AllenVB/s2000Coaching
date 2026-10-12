package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Catalogue entry the coach picks from when building a plan. Shared across all plans. */
@Entity
@Table(name = "exercise")
public class Exercise {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "muscle_group", nullable = false, length = 20)
	private MuscleGroup muscleGroup;

	@Column(name = "video_url")
	private String videoUrl;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Exercise() {
	}

	public Exercise(UUID id, String name, MuscleGroup muscleGroup, String videoUrl, String note,
			Instant createdAt) {
		this.id = id;
		this.name = name;
		this.muscleGroup = muscleGroup;
		this.videoUrl = videoUrl;
		this.note = note;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MuscleGroup getMuscleGroup() {
		return muscleGroup;
	}

	public void setMuscleGroup(MuscleGroup muscleGroup) {
		this.muscleGroup = muscleGroup;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
