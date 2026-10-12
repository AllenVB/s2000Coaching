package com.s2000coaching.web;

import com.s2000coaching.service.AdminDashboardService;
import com.s2000coaching.web.dto.AdminDashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	public AdminDashboardController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping("/api/admin/dashboard")
	public AdminDashboardResponse dashboard() {
		return adminDashboardService.load();
	}
}
