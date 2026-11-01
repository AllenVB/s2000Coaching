package com.s2000coaching.push;

import java.security.GeneralSecurityException;
import java.security.Security;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Web Push through the browsers' own push services. Off (and silent) while no VAPID key pair is configured.
 * The payload is a title, a short body and a path; it never carries health data, because it passes through
 * Google's, Apple's or Mozilla's servers on the way (encrypted, but the fact of the message is theirs to see).
 */
@Component
public class WebPushGateway implements PushGateway {

	private static final Logger log = LoggerFactory.getLogger(WebPushGateway.class);

	private final String publicKey;
	private final PushService service;

	public WebPushGateway(@Value("${app.push.public-key:}") String publicKey,
			@Value("${app.push.private-key:}") String privateKey, @Value("${app.push.subject:}") String subject) {
		PushService created = null;
		if (!publicKey.isBlank() && !privateKey.isBlank()) {
			try {
				if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
					Security.addProvider(new BouncyCastleProvider());
				}
				created = new PushService(publicKey.trim(), privateKey.trim(), subject);
			} catch (GeneralSecurityException | RuntimeException e) {
				log.error("Web Push anahtarları okunamadı; bildirimler kapalı.", e);
			}
		}
		this.service = created;
		this.publicKey = created == null ? "" : publicKey.trim();
	}

	@Override
	public boolean enabled() {
		return service != null;
	}

	@Override
	public String publicKey() {
		return publicKey;
	}

	@Override
	public Result send(String endpoint, String p256dh, String auth, String title, String body, String url) {
		if (service == null) {
			return Result.FAILED;
		}
		try {
			String payload = "{\"title\":" + quote(title) + ",\"body\":" + quote(body) + ",\"url\":" + quote(url) + "}";
			var response = service.send(new Notification(endpoint, p256dh, auth, payload));
			int status = response.getStatusLine().getStatusCode();
			if (status == 404 || status == 410) {
				return Result.GONE;
			}
			return status >= 200 && status < 300 ? Result.SENT : Result.FAILED;
		} catch (Exception e) {
			log.warn("Web Push gönderilemedi: {}", e.toString());
			return Result.FAILED;
		}
	}

	private static String quote(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
	}
}
