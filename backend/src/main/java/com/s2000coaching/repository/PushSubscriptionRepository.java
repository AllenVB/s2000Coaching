package com.s2000coaching.repository;

import com.s2000coaching.domain.PushSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

	List<PushSubscription> findAllByMemberId(UUID memberId);

	Optional<PushSubscription> findByEndpoint(String endpoint);

	void deleteAllByMemberId(UUID memberId);

	void deleteByEndpointAndMemberId(String endpoint, UUID memberId);
}
