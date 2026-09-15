package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "coaching_package_feature")
public class CoachingPackageFeature {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "package_id", nullable = false)
	private CoachingPackage coachingPackage;

	@Column(nullable = false)
	private String label;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected CoachingPackageFeature() {
	}

	public UUID getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}
}
