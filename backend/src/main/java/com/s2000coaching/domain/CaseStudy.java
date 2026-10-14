package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One published body-data analysis — the coach's own figures, shown as a case study.
 *
 * <p>Deliberately NOT built on {@link Member} and its {@link Measurement}s. The analysis is
 * served from a public endpoint, and a public endpoint that reads the member tables is one
 * forgotten filter away from publishing someone's health record. Keeping the data in its own
 * tables makes that impossible by structure rather than by care.
 *
 * <p>Nothing is public until {@link #isPublished()}: an import is not a publication.
 */
@Entity
@Table(name = "case_study")
public class CaseStudy {

	@Id
	private UUID id;

	/** Used in the public URL, so it is lower-cased and dash-separated. */
	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 2000)
	private String summary;

	/** Height, sex and birth year only exist to feed the body-fat formulas; all optional. */
	@Column(name = "height_cm")
	private Integer heightCm;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Sex sex;

	@Column(name = "birth_year")
	private Integer birthYear;

	@Column(nullable = false)
	private boolean published;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CaseStudy() {
	}

	public CaseStudy(UUID id, String slug, String title, Instant now) {
		this.id = id;
		this.slug = slug;
		this.title = title;
		this.published = false;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Integer getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(Integer heightCm) {
		this.heightCm = heightCm;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public Integer getBirthYear() {
		return birthYear;
	}

	public void setBirthYear(Integer birthYear) {
		this.birthYear = birthYear;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void touch(Instant now) {
		this.updatedAt = now;
	}
}
