package com.s2000coaching.web;

import com.s2000coaching.domain.PackageCategory;
import com.s2000coaching.service.PackageService;
import com.s2000coaching.web.dto.PackageResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PackageController {

	private final PackageService packageService;

	public PackageController(PackageService packageService) {
		this.packageService = packageService;
	}

	@GetMapping("/api/packages")
	public List<PackageResponse> listPackages(@RequestParam(required = false) PackageCategory category) {
		return packageService.listPackages(category);
	}
}
