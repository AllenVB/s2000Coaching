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

	List<Order> findAllByStatusOrderByCreatedAtAsc(com.s2000coaching.domain.OrderStatus status);

	/** The order a card provider's token was issued for. */
	Optional<Order> findByPaymentReference(String paymentReference);

	/** (number of paid orders, total in minor units) since a moment. */
	@org.springframework.data.jpa.repository.Query("""
			select count(o), coalesce(sum(o.priceMinor), 0) from Order o
			where o.createdAt >= :from and o.status in (com.s2000coaching.domain.OrderStatus.PAID, com.s2000coaching.domain.OrderStatus.MOCK_PAID)
			""")
	List<Object[]> paidSince(@org.springframework.data.repository.query.Param("from") java.time.Instant from);
}
