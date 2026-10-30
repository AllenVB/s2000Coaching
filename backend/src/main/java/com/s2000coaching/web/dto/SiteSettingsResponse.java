package com.s2000coaching.web.dto;

import java.util.List;

/**
 * What the coach has set about the public site. {@code stats} is null when the coach has not set any
 * (the site then shows its built-in ones) and an empty list when they chose to show none.
 */
public record SiteSettingsResponse(Social social, List<Stat> stats, List<LegalSummary> legal) {

	/** Each link is a ready-to-use address, or null when not set. */
	public record Social(String instagram, String youtube, String whatsapp) {
	}

	public record Stat(String label, String value, String caption, String footnote, boolean accent) {
	}

	/** {@code published} is whether the coach has written the text yet. */
	public record LegalSummary(String slug, String title, boolean published) {
	}

	/** The same, with the legal texts, for the coach's editor. */
	public record Admin(Social social, List<Stat> stats, List<LegalDocumentResponse> legal) {
	}
}
