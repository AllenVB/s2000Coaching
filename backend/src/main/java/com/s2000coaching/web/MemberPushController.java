package com.s2000coaching.web;

import com.s2000coaching.domain.Member;
import com.s2000coaching.service.MemberAuthService;
import com.s2000coaching.service.PushService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** The member's phone notifications: whether the site offers them, and turning them on or off for a device. */
@RestController
public class MemberPushController {

	private final PushService pushService;
	private final MemberAuthService memberAuthService;

	public MemberPushController(PushService pushService, MemberAuthService memberAuthService) {
		this.pushService = pushService;
		this.memberAuthService = memberAuthService;
	}

	/** {@code publicKey} is empty when the shop has not set up notifications; the page then hides the option. */
	@GetMapping("/api/member/push/config")
	public Map<String, String> config(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		memberAuthService.requireMember(authorization);
		return Map.of("publicKey", pushService.publicKey());
	}

	@PostMapping("/api/member/push/subscription")
	public ResponseEntity<Void> subscribe(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody SubscriptionRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		pushService.subscribe(member, request.endpoint(), request.p256dh(), request.auth());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/api/member/push/subscription")
	public ResponseEntity<Void> unsubscribe(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@Valid @RequestBody UnsubscribeRequest request) {
		Member member = memberAuthService.requireMember(authorization);
		pushService.unsubscribe(member, request.endpoint());
		return ResponseEntity.noContent().build();
	}

	public record SubscriptionRequest(@NotBlank @Size(max = 1000) String endpoint, @NotBlank @Size(max = 200) String p256dh,
			@NotBlank @Size(max = 100) String auth) {
	}

	public record UnsubscribeRequest(@NotBlank @Size(max = 1000) String endpoint) {
	}
}
