package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.PushSubscription;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.push.PushGateway;
import com.s2000coaching.repository.PushSubscriptionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phone notifications: which devices a member allowed, and sending to them. A device whose push service says
 * it is gone is forgotten on the spot, so a reinstalled app does not leave dead addresses behind.
 */
@Service
public class PushService implements MemberDataEraser {

	private final PushSubscriptionRepository subscriptionRepository;
	private final PushGateway gateway;

	public PushService(PushSubscriptionRepository subscriptionRepository, PushGateway gateway) {
		this.subscriptionRepository = subscriptionRepository;
		this.gateway = gateway;
	}

	public boolean enabled() {
		return gateway.enabled();
	}

	public String publicKey() {
		return gateway.publicKey();
	}

	@Transactional
	public void subscribe(Member member, String endpoint, String p256dh, String auth) {
		if (!gateway.enabled()) {
			throw new BadRequestException("Bildirimler şu an kapalı.");
		}
		if (!endpoint.startsWith("https://")) {
			throw new BadRequestException("Geçersiz bildirim adresi.");
		}
		subscriptionRepository.findByEndpoint(endpoint).ifPresentOrElse(
				existing -> existing.rebind(member, p256dh, auth),
				() -> subscriptionRepository.save(
						new PushSubscription(UUID.randomUUID(), member, endpoint, p256dh, auth, Instant.now())));
	}

	@Transactional
	public void unsubscribe(Member member, String endpoint) {
		subscriptionRepository.deleteByEndpointAndMemberId(endpoint, member.getId());
	}

	@Transactional(readOnly = true)
	public boolean hasDevice(UUID memberId) {
		return !subscriptionRepository.findAllByMemberId(memberId).isEmpty();
	}

	/** @return true when at least one of the member's devices accepted the message */
	@Transactional
	public boolean notify(Member member, String title, String body, String url) {
		if (!gateway.enabled()) {
			return false;
		}
		boolean delivered = false;
		for (PushSubscription subscription : subscriptionRepository.findAllByMemberId(member.getId())) {
			var result = gateway.send(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(),
					title, body, url);
			if (result == PushGateway.Result.SENT) {
				delivered = true;
			} else if (result == PushGateway.Result.GONE) {
				subscriptionRepository.delete(subscription);
			}
		}
		return delivered;
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		subscriptionRepository.deleteAllByMemberId(memberId);
	}
}
