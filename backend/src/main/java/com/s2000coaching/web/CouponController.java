package com.s2000coaching.web;

import com.s2000coaching.service.CouponService;
import com.s2000coaching.web.dto.CouponDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Discount codes: public to check one against a package, coach-only to manage. */
@RestController
public class CouponController {

	private final CouponService couponService;

	public CouponController(CouponService couponService) {
		this.couponService = couponService;
	}

	@PostMapping("/api/coupons/validate")
	public CouponDtos.Validation validate(@Valid @RequestBody CouponDtos.ValidateRequest request,
			HttpServletRequest http) {
		return couponService.validate(request, clientAddress(http));
	}

	@GetMapping("/api/admin/coupons")
	public List<CouponDtos.Response> list() {
		return couponService.list();
	}

	@PostMapping("/api/admin/coupons")
	public CouponDtos.Response create(@Valid @RequestBody CouponDtos.CreateRequest request) {
		return couponService.create(request);
	}

	@PutMapping("/api/admin/coupons/{id}/active")
	public CouponDtos.Response setActive(@PathVariable UUID id, @RequestParam boolean active) {
		return couponService.setActive(id, active);
	}

	private static String clientAddress(HttpServletRequest http) {
		String forwarded = http.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return http.getRemoteAddr();
	}
}
