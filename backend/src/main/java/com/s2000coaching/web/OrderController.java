package com.s2000coaching.web;

import com.s2000coaching.service.OrderService;
import com.s2000coaching.web.dto.CreateOrderRequest;
import com.s2000coaching.web.dto.OrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/api/orders")
	public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
			HttpServletRequest http) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, clientIp(http)));
	}

	@GetMapping("/api/orders/{orderNumber}")
	public OrderResponse getOrder(@PathVariable String orderNumber) {
		return orderService.getByOrderNumber(orderNumber);
	}

	/**
	 * The buyer's address, for the card provider's fraud checks. Behind a reverse proxy the real
	 * client is the first entry of X-Forwarded-For; otherwise it is the connection's own address.
	 * Informational only — nothing here is a security decision.
	 */
	private static String clientIp(HttpServletRequest http) {
		String forwarded = http.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return http.getRemoteAddr();
	}
}
