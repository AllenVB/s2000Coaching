package com.s2000coaching.web.dto;

import com.s2000coaching.domain.Coupon;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public final class CouponDtos {

	private CouponDtos() {
	}

	public record ValidateRequest(@NotBlank @Size(max = 40) String code, @NotNull UUID packageId) {
	}

	/** What the buyer sees: the code is good, and what the package costs with it. */
	public record Validation(String code, int percentOff, long originalMinor, long discountedMinor) {
	}

	public record CreateRequest(
			@NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Kod yalnızca harf, rakam, - ve _ içerebilir") String code,
			@Min(1) @Max(90) int percentOff,
			@Min(1) @Max(100000) Integer maxUses,
			LocalDate expiresOn) {
	}

	public record Response(UUID id, String code, int percentOff, Integer maxUses, int usedCount, LocalDate expiresOn,
			boolean active) {

		public static Response from(Coupon coupon) {
			return new Response(coupon.getId(), coupon.getCode(), coupon.getPercentOff(), coupon.getMaxUses(),
					coupon.getUsedCount(), coupon.getExpiresOn(), coupon.isActive());
		}
	}
}
