package com.s2000coaching.web;

import com.s2000coaching.service.SiteSettingsService;
import com.s2000coaching.web.dto.LegalDocumentResponse;
import com.s2000coaching.web.dto.SiteSettingsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The public site's editable facts: social links, home-page figures and the legal pages. Reads no member data. */
@RestController
public class SiteController {

	private final SiteSettingsService service;

	public SiteController(SiteSettingsService service) {
		this.service = service;
	}

	@GetMapping("/api/site")
	public SiteSettingsResponse site() {
		return service.publicSettings();
	}

	@GetMapping("/api/site/legal/{slug}")
	public LegalDocumentResponse legal(@PathVariable String slug) {
		return service.legalDocument(slug);
	}
}
