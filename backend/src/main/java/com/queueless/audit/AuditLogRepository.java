package com.queueless.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
    Page<AuditLog> findByActorUserIdOrderByOccurredAtDesc(Long actorUserId, Pageable pageable);
    Page<AuditLog> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(String targetType, Long targetId, Pageable pageable);
}
