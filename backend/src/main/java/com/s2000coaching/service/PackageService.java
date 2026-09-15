package com.s2000coaching.service;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CoachingPackageRepository;
import com.s2000coaching.web.dto.PackageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageService {

	private final CoachingPackageRepository packageRepository;

	public PackageService(CoachingPackageRepository packageRepository) {
		this.packageRepository = packageRepository;
	}

	@Transactional(readOnly = true)
	public List<PackageResponse> listPackages(PackageCategory category) {
		var packages = category == null
				? packageRepository.findByActiveTrueOrderByDisplayOrderAsc()
				: packageRepository.findByActiveTrueAndCategoryOrderByDisplayOrderAsc(category);
		return packages.stream().map(PackageResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public CoachingPackage getActiveOrThrow(UUID id) {
		return packageRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new NotFoundException("Paket bulunamadı: " + id));
	}
}
