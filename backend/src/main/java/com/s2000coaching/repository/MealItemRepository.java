package com.s2000coaching.repository;

import com.s2000coaching.domain.MealItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealItemRepository extends JpaRepository<MealItem, UUID> {
}
