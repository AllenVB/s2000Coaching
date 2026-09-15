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
		@AssertTrue(message = "KVKK onamı zorunludur") boolean kvkkConsent) {
}
