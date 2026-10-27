package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A member's transformation, written up by the coach and shown publicly — only with the
 * member's consent to exactly this content.
 *
 * <p>Not built on {@link ProgressPhoto}: the photos are copied into {@link SuccessStoryPhoto}
 * rows of their own, so the public endpoints never touch a member table (see the case-study
 * design for the same reasoning).
 */
@Entity
@Table(name = "success_story")
public class SuccessStory {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 160)
	private String title;

	/** One sentence in the member's own words, optional. */
	@Column(length = 400)
	private String quote;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	/** What the site shows instead of a full name, e.g. "Ahmet Y." — chosen with the member. */
	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StoryStatus status;

	@Column(name = "consent_at")
	private Instant consentAt;

	/** The exact wording the member agreed to, kept as evidence of what they consented to. */
	@Column(name = "consent_text", columnDefinition = "text")
	private String consentText;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SuccessStory() {
	}

	public SuccessStory(UUID id, Member member, String title, String quote, String body, String displayName,
			Instant now) {
		this.id = id;
		this.member = member;
		this.title = title;
		this.quote = quote;
		this.body = body;
		this.displayName = displayName;
		this.status = StoryStatus.DRAFT;
		this.createdAt = now;
		this.updatedAt = now;
	}

	/**
	 * Changing the words or the photos voids any consent given: the member agreed to what they
	 * saw, not to whatever it became. The story goes back to a draft and has to be sent again.
	 */
	public void changeContent(String title, String quote, String body, String displayName, Instant now) {
		this.title = title;
		this.quote = quote;
		this.body = body;
		this.displayName = displayName;
		voidConsent(now);
	}

	public void voidConsent(Instant now) {
		if (status == StoryStatus.DECLINED || status == StoryStatus.REVOKED) {
			this.updatedAt = now;
			return;
		}
		this.status = StoryStatus.DRAFT;
		this.consentAt = null;
		this.consentText = null;
		this.publishedAt = null;
		this.updatedAt = now;
	}

	public void sendForConsent(Instant now) {
		this.status = StoryStatus.AWAITING_CONSENT;
		this.updatedAt = now;
	}

	public void consent(String text, Instant now) {
		this.status = StoryStatus.CONSENTED;
		this.consentAt = now;
		this.consentText = text;
		this.updatedAt = now;
	}

	public void decline(Instant now) {
		this.status = StoryStatus.DECLINED;
		this.updatedAt = now;
	}

	public void publish(Instant now) {
		this.status = StoryStatus.PUBLISHED;
		this.publishedAt = now;
		this.updatedAt = now;
	}

	public void unpublish(Instant now) {
		this.status = StoryStatus.CONSENTED;
		this.publishedAt = null;
		this.updatedAt = now;
	}

	public void revoke(Instant now) {
		this.status = StoryStatus.REVOKED;
		this.revokedAt = now;
		this.publishedAt = null;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getTitle() {
		return title;
	}

	public String getQuote() {
		return quote;
	}

	public String getBody() {
		return body;
	}

	public String getDisplayName() {
		return displayName;
	}

	public StoryStatus getStatus() {
		return status;
	}

	public Instant getConsentAt() {
		return consentAt;
	}

	public String getConsentText() {
		return consentText;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
