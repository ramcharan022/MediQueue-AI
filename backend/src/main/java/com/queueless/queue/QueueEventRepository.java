package com.queueless.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QueueEventRepository extends JpaRepository<QueueEvent, Long> {

    List<QueueEvent> findByTokenIdOrderByOccurredAtAsc(Long tokenId);

    @Query("SELECT e FROM QueueEvent e ORDER BY e.occurredAt DESC")
    List<QueueEvent> findRecentEvents(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(e) FROM QueueEvent e WHERE e.nextStatus = 'COMPLETED' AND e.occurredAt >= :since")
    long countCompletedSince(@Param("since") java.time.Instant since);
}
