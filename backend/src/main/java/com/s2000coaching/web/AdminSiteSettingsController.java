package com.s2000coaching.web;

import com.s2000coaching.service.SiteSettingsService;
import com.s2000coaching.web.dto.SiteSettingsResponse;
import com.s2000coaching.web.dto.UpdateSiteSettingsRequests;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The coach's editor for what the public site shows. Every route is behind the coach session. */
@RestController
public class AdminSiteSettingsController {

	private final SiteSettingsService service;

	public AdminSiteSettingsController(SiteSettingsService service) {
		this.service = service;
	}

	@GetMapping("/api/admin/site-settings")
	public SiteSettingsResponse.Admin get() {
		return service.adminSettings();
	}

	@PutMapping("/api/admin/site-settings/social")
	public SiteSettingsResponse.Admin updateSocial(@Valid @RequestBody UpdateSiteSettingsRequests.Social request) {
		return service.updateSocial(request);
	}

	@PutMapping("/api/admin/site-settings/stats")
	public SiteSettingsResponse.Admin updateStats(@Valid @RequestBody UpdateSiteSettingsRequests.Stats request) {
		return service.updateStats(request);
	}

	@PutMapping("/api/admin/site-settings/legal/{slug}")
	public SiteSettingsResponse.Admin updateLegal(@PathVariable String slug,
			@Valid @RequestBody UpdateSiteSettingsRequests.Legal request) {
		return service.updateLegal(slug, request);
	}
}
