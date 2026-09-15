package com.s2000coaching.repository;

import com.s2000coaching.domain.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	Optional<Order> findByOrderNumber(String orderNumber);
}
