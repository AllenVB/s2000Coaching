package com.s2000coaching.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOrderRequest(
		@NotBlank @Size(max = 160) String customerName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 40) String phone,
		@Size(max = 120) String cityAge,
		@NotNull UUID packageId,
		@AssertTrue(message = "KVKK onamı zorunludur") boolean kvkkConsent,
		/** Null means the first available method. */
		com.s2000coaching.domain.PaymentMethod paymentMethod,
		/** Optional discount code; blank means none. */
		@Size(max = 40) String couponCode) {

	/** The shape from before discount codes existed. */
	public CreateOrderRequest(String customerName, String email, String phone, String cityAge, UUID packageId,
			boolean kvkkConsent, com.s2000coaching.domain.PaymentMethod paymentMethod) {
		this(customerName, email, phone, cityAge, packageId, kvkkConsent, paymentMethod, null);
	}
}
