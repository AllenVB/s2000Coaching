package com.s2000coaching.repository;

import com.s2000coaching.domain.MessageTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {

	List<MessageTemplate> findAllByOrderByTitleAsc();

	boolean existsByTitleIgnoreCase(String title);
}
