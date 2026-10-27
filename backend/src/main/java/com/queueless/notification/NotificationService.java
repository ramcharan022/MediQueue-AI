package com.queueless.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * In-app notification service.
 * Designed so email/SMS providers can be added later without changing callers.
 * Runs in REQUIRES_NEW so a notification failure never rolls back the main transaction.
 */
@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(Long userId, String type, String title, String body, String reference) {
        // Deduplication: skip if the same type+reference notification already exists
        if (reference != null && repo.existsByUserIdAndTypeAndReference(userId, type, reference)) {
            return;
        }
        repo.save(new Notification(userId, type, title, body, reference));
    }

    public List<Notification> getForUser(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(Long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        repo.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        repo.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId) && !n.isRead()) {
                n.setReadAt(Instant.now());
                repo.save(n);
            }
        });
    }
}
