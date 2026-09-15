package com.s2000coaching.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "coaching_package")
public class CoachingPackage {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String slug;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PackageCategory category;

	@Column(nullable = false)
	private String name;

	private String tagline;

	@Column(name = "duration_months", nullable = false)
	private int durationMonths;

	@Column(name = "price_minor", nullable = false)
	private long priceMinor;

	@Column(name = "original_price_minor", nullable = false)
	private long originalPriceMinor;

	private String badge;

	@Column(nullable = false)
	private boolean featured;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private boolean active;

	@OneToMany(mappedBy = "coachingPackage", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder asc")
	private List<CoachingPackageFeature> features = new ArrayList<>();

	protected CoachingPackage() {
	}

	public CoachingPackage(UUID id, String slug, PackageCategory category, String name, String tagline,
			int durationMonths, long priceMinor, long originalPriceMinor, String badge, boolean featured,
			int displayOrder, boolean active) {
		this.id = id;
		this.slug = slug;
		this.category = category;
		this.name = name;
		this.tagline = tagline;
		this.durationMonths = durationMonths;
		this.priceMinor = priceMinor;
		this.originalPriceMinor = originalPriceMinor;
		this.badge = badge;
		this.featured = featured;
		this.displayOrder = displayOrder;
		this.active = active;
	}

	public UUID getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public PackageCategory getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getTagline() {
		return tagline;
	}

	public int getDurationMonths() {
		return durationMonths;
	}

	public long getPriceMinor() {
		return priceMinor;
	}

	public long getOriginalPriceMinor() {
		return originalPriceMinor;
	}

	public String getBadge() {
		return badge;
	}

	public boolean isFeatured() {
		return featured;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public boolean isActive() {
		return active;
	}

	public List<CoachingPackageFeature> getFeatures() {
		return features;
	}
}
