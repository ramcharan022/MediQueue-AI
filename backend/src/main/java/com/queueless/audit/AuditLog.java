package com.queueless.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log",
       indexes = {
           @Index(name = "idx_audit_log_occurred", columnList = "occurred_at DESC"),
           @Index(name = "idx_audit_log_target", columnList = "target_type, target_id")
       })
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", length = 60)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 1000)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public AuditLog() {}

    public AuditLog(Long actorUserId, String action, String targetType, Long targetId, String detail) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
