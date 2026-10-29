package com.s2000coaching.web;

import com.s2000coaching.service.IyzicoCallbackService;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Where iyzico sends the buyer's browser after the card page. Public by necessity; it is safe
 * because the posted token is only a lookup key — the result is fetched from iyzico, not taken
 * from the request (see {@link IyzicoCallbackService}).
 */
@RestController
public class IyzicoCallbackController {

	private final IyzicoCallbackService callbackService;
	private final String siteUrl;

	public IyzicoCallbackController(IyzicoCallbackService callbackService, @Value("${app.public-url}") String siteUrl) {
		this.callbackService = callbackService;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
	}

	@PostMapping(value = "/api/payments/iyzico/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Void> callback(@RequestParam(value = "token", required = false) String token) {
		IyzicoCallbackService.Result result = callbackService.complete(token);
		var target = UriComponentsBuilder.fromUriString(siteUrl + "/")
				.queryParam("odeme", switch (result.outcome()) {
					case PAID -> "basarili";
					case FAILED -> "basarisiz";
					case UNKNOWN -> "belirsiz";
				});
		if (result.orderNumber() != null) {
			target.queryParam("siparis", result.orderNumber());
		}
		return ResponseEntity.status(HttpStatus.SEE_OTHER)
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.location(URI.create(target.build().toUriString()))
				.build();
	}
}
