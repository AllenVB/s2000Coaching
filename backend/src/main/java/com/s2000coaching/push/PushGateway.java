package com.s2000coaching.push;

/** Sends one Web Push message. The real one needs VAPID keys; tests use a fake. */
public interface PushGateway {

	enum Result {
		/** The push service accepted it. */
		SENT,
		/** The device is gone (HTTP 404/410): the subscription should be deleted. */
		GONE,
		/** Anything else: try again another time, keep the subscription. */
		FAILED
	}

	boolean enabled();

	/** The VAPID public key the browser needs to subscribe; empty when push is off. */
	String publicKey();

	Result send(String endpoint, String p256dh, String auth, String title, String body, String url);
}
