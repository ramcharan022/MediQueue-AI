package com.queueless.queue;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "queue_event")
public class QueueEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_id", nullable = false) private Long tokenId;
    @Column(name = "actor_user_id") private Long actorUserId;
    @Column(name = "previous_status", nullable = false, length = 20) private String previousStatus;
    @Column(name = "next_status", nullable = false, length = 20) private String nextStatus;
    @Column(length = 500) private String reason;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt = Instant.now();

    public QueueEvent() {}
    public QueueEvent(Long tokenId, Long actorUserId, String prev, String next, String reason) {
        this.tokenId = tokenId;
        this.actorUserId = actorUserId;
        this.previousStatus = prev;
        this.nextStatus = next;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getTokenId() { return tokenId; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNextStatus() { return nextStatus; }
    public void setNextStatus(String nextStatus) { this.nextStatus = nextStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getOccurredAt() { return occurredAt; }
}
