package com.s2000coaching.web.dto;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.PackageCategory;
import java.util.List;
import java.util.UUID;

public record PackageResponse(
		UUID id,
		String slug,
		PackageCategory category,
		String name,
		String tagline,
		int durationMonths,
		long priceMinor,
		long originalPriceMinor,
		String badge,
		boolean featured,
		List<PackageFeatureResponse> features) {

	public static PackageResponse from(CoachingPackage coachingPackage) {
		List<PackageFeatureResponse> features = coachingPackage.getFeatures().stream()
				.map(feature -> new PackageFeatureResponse(feature.getLabel()))
				.toList();
		return new PackageResponse(
				coachingPackage.getId(),
				coachingPackage.getSlug(),
				coachingPackage.getCategory(),
				coachingPackage.getName(),
				coachingPackage.getTagline(),
				coachingPackage.getDurationMonths(),
				coachingPackage.getPriceMinor(),
				coachingPackage.getOriginalPriceMinor(),
				coachingPackage.getBadge(),
				coachingPackage.isFeatured(),
				features);
	}
}
