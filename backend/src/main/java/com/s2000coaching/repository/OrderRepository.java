package com.s2000coaching.repository;

import com.s2000coaching.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	Optional<Order> findByOrderNumber(String orderNumber);

	List<Order> findTop10ByOrderByCreatedAtDesc();

	List<Order> findAllByOrderByCreatedAtDesc();

	List<Order> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

	/** Orders placed with this address that are not attached to a member yet. */
	List<Order> findAllByEmailIgnoreCaseAndMemberIsNull(String email);
}
