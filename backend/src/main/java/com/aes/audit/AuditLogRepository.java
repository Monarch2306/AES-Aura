package com.aes.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
	List<AuditLog> findByActionContainingIgnoreCaseOrEntityTypeContainingIgnoreCase(String action, String entityType, Pageable pageable);
}
