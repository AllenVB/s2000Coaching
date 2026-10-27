package com.s2000coaching.web;

import com.s2000coaching.service.AdminOrderService;
import com.s2000coaching.web.dto.OrderSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminOrderController {

	private final AdminOrderService adminOrderService;

	public AdminOrderController(AdminOrderService adminOrderService) {
		this.adminOrderService = adminOrderService;
	}

	@GetMapping("/api/admin/orders")
	public List<OrderSummaryResponse> list() {
		return adminOrderService.listAll();
	}

	/** Used to prefill the member form when turning an order into a membership. */
	@GetMapping("/api/admin/orders/{orderNumber}")
	public OrderSummaryResponse get(@PathVariable String orderNumber) {
		return adminOrderService.getByOrderNumber(orderNumber);
	}

	/** The money arrived: marks the order paid and grants the membership it bought. */
	@PostMapping("/api/admin/orders/{orderNumber}/confirm-payment")
	public OrderSummaryResponse confirmPayment(@PathVariable String orderNumber) {
		return adminOrderService.confirmPayment(orderNumber);
	}

	@PostMapping("/api/admin/orders/{orderNumber}/cancel")
	public OrderSummaryResponse cancel(@PathVariable String orderNumber) {
		return adminOrderService.cancel(orderNumber);
	}

	@GetMapping("/api/admin/members/{memberId}/orders")
	public List<OrderSummaryResponse> listForMember(@PathVariable UUID memberId) {
		return adminOrderService.listForMember(memberId);
	}
}
