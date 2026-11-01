package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One device a member allowed notifications on. */
@Entity
@Table(name = "push_subscription")
public class PushSubscription {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, unique = true, length = 1000)
	private String endpoint;

	@Column(nullable = false, length = 200)
	private String p256dh;

	@Column(nullable = false, length = 100)
	private String auth;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PushSubscription() {
	}

	public PushSubscription(UUID id, Member member, String endpoint, String p256dh, String auth, Instant now) {
		this.id = id;
		this.member = member;
		this.endpoint = endpoint;
		this.p256dh = p256dh;
		this.auth = auth;
		this.createdAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getP256dh() {
		return p256dh;
	}

	public String getAuth() {
		return auth;
	}

	/** The same device re-subscribing (e.g. after the browser rotated its keys) under a possibly different member. */
	public void rebind(Member member, String p256dh, String auth) {
		this.member = member;
		this.p256dh = p256dh;
		this.auth = auth;
	}
}
