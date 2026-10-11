package com.s2000coaching.repository;

import com.s2000coaching.domain.Meal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, UUID> {
}
