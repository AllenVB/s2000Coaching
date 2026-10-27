package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A photo of the coach's own body, belonging to one case study. Not a {@link ProgressPhoto}:
 * those belong to members and must never be reachable from a public endpoint. The bytes live
 * in {@code PhotoStorage}; only the key is kept here.
 *
 * <p>Held by id rather than a {@code @ManyToOne} to {@link CaseStudy}: nothing needs the
 * study through the photo, and a lazy association is one more way to leak a draft.
 */
@Entity
@Table(name = "case_study_photo")
public class CaseStudyPhoto {

	@Id
	private UUID id;

	@Column(name = "case_study_id", nullable = false)
	private UUID caseStudyId;

	/** The day it was taken, which is what the period matching uses — not the upload time. */
	@Column(name = "taken_on", nullable = false)
	private LocalDate takenOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PhotoPose pose;

	@Column(name = "storage_key", nullable = false, length = 300)
	private String storageKey;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected CaseStudyPhoto() {
	}

	public CaseStudyPhoto(UUID id, UUID caseStudyId, LocalDate takenOn, PhotoPose pose, String storageKey,
			String contentType, long sizeBytes, Instant createdAt) {
		this.id = id;
		this.caseStudyId = caseStudyId;
		this.takenOn = takenOn;
		this.pose = pose;
		this.storageKey = storageKey;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getCaseStudyId() {
		return caseStudyId;
	}

	public LocalDate getTakenOn() {
		return takenOn;
	}

	public PhotoPose getPose() {
		return pose;
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

	public Instant getCreatedAt() {
		return createdAt;
	}
}
