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

@Entity
@Table(name = "customer_order")
public class Order {

	@Id
	private UUID id;

	@Column(name = "order_number", nullable = false, unique = true)
	private String orderNumber;

	@ManyToOne
	@JoinColumn(name = "package_id", nullable = false)
	private CoachingPackage coachingPackage;

	@Column(name = "customer_name", nullable = false)
	private String customerName;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String phone;

	@Column(name = "city_age")
	private String cityAge;

	@Column(name = "price_minor", nullable = false)
	private long priceMinor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(name = "kvkk_consent", nullable = false)
	private boolean kvkkConsent;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Order() {
	}

	public Order(UUID id, String orderNumber, CoachingPackage coachingPackage, String customerName, String email,
			String phone, String cityAge, long priceMinor, OrderStatus status, boolean kvkkConsent,
			Instant createdAt) {
		this.id = id;
		this.orderNumber = orderNumber;
		this.coachingPackage = coachingPackage;
		this.customerName = customerName;
		this.email = email;
		this.phone = phone;
		this.cityAge = cityAge;
		this.priceMinor = priceMinor;
		this.status = status;
		this.kvkkConsent = kvkkConsent;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public CoachingPackage getCoachingPackage() {
		return coachingPackage;
	}

	public String getCustomerName() {
		return customerName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getCityAge() {
		return cityAge;
	}

	public long getPriceMinor() {
		return priceMinor;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public boolean isKvkkConsent() {
		return kvkkConsent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
