package com.queueless.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification",
       indexes = @Index(name = "idx_notification_user_unread", columnList = "user_id, read_at, created_at"))
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(length = 40)
    private String reference;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification() {}

    public Notification(Long userId, String type, String title, String body, String reference) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.reference = reference;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getReference() { return reference; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return readAt != null; }
}
