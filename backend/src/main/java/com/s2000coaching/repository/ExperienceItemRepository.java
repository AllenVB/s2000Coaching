package com.s2000coaching.repository;

import com.s2000coaching.domain.ExperienceItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceItemRepository extends JpaRepository<ExperienceItem, UUID> {
}
