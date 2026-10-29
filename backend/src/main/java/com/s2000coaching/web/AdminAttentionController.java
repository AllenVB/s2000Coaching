package com.s2000coaching.web;

import com.s2000coaching.service.AttentionService;
import com.s2000coaching.web.dto.AttentionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The coach's "who needs me today" list. Behind the coach session gate in {@code WebConfig}. */
@RestController
public class AdminAttentionController {

	private final AttentionService attentionService;

	public AdminAttentionController(AttentionService attentionService) {
		this.attentionService = attentionService;
	}

	@GetMapping("/api/admin/attention")
	public AttentionResponse load() {
		return attentionService.load();
	}
}
