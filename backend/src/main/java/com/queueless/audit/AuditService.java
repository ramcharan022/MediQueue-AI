package com.queueless.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records administrative and queue operations for compliance and debugging.
 * Uses REQUIRES_NEW so audit entries are written even if the caller's transaction rolls back.
 */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long actorUserId, String action, String targetType, Long targetId, String detail) {
        repo.save(new AuditLog(actorUserId, action, targetType, targetId, detail));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String targetType, Long targetId, String detail) {
        log(null, action, targetType, targetId, detail);
    }
}
