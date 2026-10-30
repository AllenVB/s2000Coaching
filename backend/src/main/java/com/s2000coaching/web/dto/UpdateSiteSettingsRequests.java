package com.s2000coaching.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The bodies of the three things the coach can change about the public site. */
public final class UpdateSiteSettingsRequests {

	private UpdateSiteSettingsRequests() {
	}

	/** Blank clears a link. Instagram takes a user name or a link, WhatsApp a phone number or a wa.me link. */
	public record Social(
			@Size(max = 200) String instagram,
			@Size(max = 200) String youtube,
			@Size(max = 200) String whatsapp) {
	}

	/** {@code stats} null puts the built-in figures back; an empty list shows none. */
	public record Stats(@Size(max = 4) List<@Valid Stat> stats) {
	}

	public record Stat(
			@NotBlank @Size(max = 40) String label,
			@NotBlank @Size(max = 20) String value,
			@NotBlank @Size(max = 60) String caption,
			@Size(max = 200) String footnote,
			boolean accent) {
	}

	/** Blank unpublishes the text. */
	public record Legal(@Size(max = 100_000) String content) {
	}
}
