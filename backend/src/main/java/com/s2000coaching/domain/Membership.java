package com.s2000coaching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A period a member has paid for. Created from a confirmed order, or by the coach for money
 * that arrived some other way (cash, a transfer made before the site existed). A member's
 * current period is the one that ends last.
 */
@Entity
@Table(name = "membership")
public class Membership {

	@Id
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** The order this came from; null for one the coach added by hand. Unique when set. */
	@ManyToOne
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(name = "starts_on", nullable = false)
	private LocalDate startsOn;

	@Column(name = "ends_on", nullable = false)
	private LocalDate endsOn;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Membership() {
	}

	public Membership(UUID id, Member member, Order order, String label, LocalDate startsOn, LocalDate endsOn,
			Instant createdAt) {
		this.id = id;
		this.member = member;
		this.order = order;
		this.label = label;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Order getOrder() {
		return order;
	}

	public String getLabel() {
		return label;
	}

	public LocalDate getStartsOn() {
		return startsOn;
	}

	public LocalDate getEndsOn() {
		return endsOn;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
