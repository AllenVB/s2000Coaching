package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One week of a member's eating: the week's total calories, the share of them that came from
 * carbohydrate, fat and protein, and at most one photo. {@code weekStart} is the Monday and never
 * changes once the row exists. Everything the row needs is passed to the constructor, because an
 * insert is built from the entity as it is when first persisted.
 */
@Entity
@Table(name = "weekly_nutrition")
public class WeeklyNutrition {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "week_start", nullable = false, updatable = false)
	private LocalDate weekStart;

	@Column(nullable = false)
	private int calories;

	@Column(name = "carbs_percent", nullable = false)
	private int carbsPercent;

	@Column(name = "fat_percent", nullable = false)
	private int fatPercent;

	@Column(name = "protein_percent", nullable = false)
	private int proteinPercent;

	@Column(name = "photo_key", length = 200)
	private String photoKey;

	@Column(name = "photo_content_type", length = 50)
	private String photoContentType;

	@Column(name = "photo_size_bytes")
	private Long photoSizeBytes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected WeeklyNutrition() {
	}

	public WeeklyNutrition(UUID id, Member member, LocalDate weekStart, int calories, int carbsPercent,
			int fatPercent, int proteinPercent, Instant now) {
		this.id = id;
		this.member = member;
		this.weekStart = weekStart;
		this.calories = calories;
		this.carbsPercent = carbsPercent;
		this.fatPercent = fatPercent;
		this.proteinPercent = proteinPercent;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void update(int calories, int carbsPercent, int fatPercent, int proteinPercent, Instant now) {
		this.calories = calories;
		this.carbsPercent = carbsPercent;
		this.fatPercent = fatPercent;
		this.proteinPercent = proteinPercent;
		this.updatedAt = now;
	}

	public void attachPhoto(String key, String contentType, long sizeBytes) {
		this.photoKey = key;
		this.photoContentType = contentType;
		this.photoSizeBytes = sizeBytes;
	}

	public void clearPhoto() {
		this.photoKey = null;
		this.photoContentType = null;
		this.photoSizeBytes = null;
	}

	public boolean hasPhoto() {
		return photoKey != null;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public LocalDate getWeekStart() {
		return weekStart;
	}

	public int getCalories() {
		return calories;
	}

	public int getCarbsPercent() {
		return carbsPercent;
	}

	public int getFatPercent() {
		return fatPercent;
	}

	public int getProteinPercent() {
		return proteinPercent;
	}

	public String getPhotoKey() {
		return photoKey;
	}

	public String getPhotoContentType() {
		return photoContentType;
	}

	public Long getPhotoSizeBytes() {
		return photoSizeBytes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
