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
import java.time.LocalDate;
import java.util.UUID;

/**
 * One progress photo. The bytes live in {@code PhotoStorage} and only the key is kept here,
 * so backups and queries stay the size of the metadata rather than the images.
 *
 * <p>Health data under KVKK, like {@link Measurement} and more so: a body photo identifies
 * the person in it. Only the member and their coach can read one, and the member must have
 * given explicit consent ({@link Member#getHealthDataConsentAt()}) before any row is written.
 */
@Entity
@Table(name = "progress_photo")
public class ProgressPhoto {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** The day the photo was taken, which is what the comparison orders by — not upload time. */
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

	@Column(length = 2000)
	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ProgressPhoto() {
	}

	public ProgressPhoto(UUID id, Member member, LocalDate takenOn, PhotoPose pose, String storageKey,
			String contentType, long sizeBytes, String note, Instant createdAt) {
		this.id = id;
		this.member = member;
		this.takenOn = takenOn;
		this.pose = pose;
		this.storageKey = storageKey;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.note = note;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
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
