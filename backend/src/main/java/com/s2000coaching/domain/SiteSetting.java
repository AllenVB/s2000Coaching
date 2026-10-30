package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One editable fact about the public site, keyed by name; see {@code SiteSettingsService} for the keys. */
@Entity
@Table(name = "site_setting")
public class SiteSetting {

	@Id
	@Column(name = "setting_key", length = 60)
	private String key;

	@Column(name = "setting_value", nullable = false, columnDefinition = "text")
	private String value;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SiteSetting() {
	}

	public SiteSetting(String key, String value, Instant updatedAt) {
		this.key = key;
		this.value = value;
		this.updatedAt = updatedAt;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void change(String value, Instant now) {
		this.value = value;
		this.updatedAt = now;
	}
}
