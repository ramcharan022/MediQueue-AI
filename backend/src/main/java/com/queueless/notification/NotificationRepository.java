package com.queueless.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :uid AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnread(@Param("uid") Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :uid AND n.readAt IS NULL")
    int markAllRead(@Param("uid") Long userId, @Param("now") Instant now);

    // Deduplication: avoid creating the same type+reference notification twice
    boolean existsByUserIdAndTypeAndReference(Long userId, String type, String reference);
}
