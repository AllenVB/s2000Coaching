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
	/** Null for an exercise the coach simply typed into a plan; the catalogue page can fill it in later. */
	@Column(name = "muscle_group", length = 20)
	private MuscleGroup muscleGroup;

	@Column(name = "video_url")
	private String videoUrl;

	@Column(columnDefinition = "text")
	private String note;

	/** The demonstration image the coach uploaded (storage key and file facts); all null when there is none. */
	@Column(name = "image_key", length = 200)
	private String imageKey;

	@Column(name = "image_content_type", length = 50)
	private String imageContentType;

	@Column(name = "image_size_bytes")
	private Long imageSizeBytes;

	@Column(name = "image_updated_at")
	private Instant imageUpdatedAt;

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

	public boolean hasImage() {
		return imageKey != null;
	}

	public String getImageKey() {
		return imageKey;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public Long getImageSizeBytes() {
		return imageSizeBytes;
	}

	public Instant getImageUpdatedAt() {
		return imageUpdatedAt;
	}

	public void attachImage(String key, String contentType, long sizeBytes, Instant now) {
		this.imageKey = key;
		this.imageContentType = contentType;
		this.imageSizeBytes = sizeBytes;
		this.imageUpdatedAt = now;
	}

	public void clearImage() {
		this.imageKey = null;
		this.imageContentType = null;
		this.imageSizeBytes = null;
		this.imageUpdatedAt = null;
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
