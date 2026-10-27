package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A photo copied into a success story. Held by id, like {@link CaseStudyPhoto}. */
@Entity
@Table(name = "success_story_photo")
public class SuccessStoryPhoto {

	@Id
	private UUID id;

	@Column(name = "story_id", nullable = false)
	private UUID storyId;

	@Column(name = "taken_on", nullable = false)
	private LocalDate takenOn;

	@Column(nullable = false, length = 60)
	private String caption;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "storage_key", nullable = false, length = 300)
	private String storageKey;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected SuccessStoryPhoto() {
	}

	public SuccessStoryPhoto(UUID id, UUID storyId, LocalDate takenOn, String caption, int displayOrder,
			String storageKey, String contentType, long sizeBytes, Instant createdAt) {
		this.id = id;
		this.storyId = storyId;
		this.takenOn = takenOn;
		this.caption = caption;
		this.displayOrder = displayOrder;
		this.storageKey = storageKey;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getStoryId() {
		return storyId;
	}

	public LocalDate getTakenOn() {
		return takenOn;
	}

	public String getCaption() {
		return caption;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}
}
