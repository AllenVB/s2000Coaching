package com.s2000coaching.repository;

import com.s2000coaching.domain.Exercise;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

	List<Exercise> findAllByOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);

	java.util.Optional<com.s2000coaching.domain.Exercise> findFirstByNameIgnoreCase(String name);
}
