package com.queueless.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    List<QueueToken> findByQueueIdOrderByCreatedAtAsc(Long queueId);

    List<QueueToken> findByQueueIdAndStatusOrderByCreatedAtAsc(Long queueId, TokenStatus status);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.queueId = :qId AND t.status = 'WAITING'")
    long countWaiting(@Param("qId") Long queueId);

    @Query("SELECT t FROM QueueToken t WHERE t.queueId = :qId AND t.status = 'WAITING' ORDER BY t.createdAt ASC")
    List<QueueToken> findWaitingOrdered(@Param("qId") Long queueId);

    @Query("SELECT t FROM QueueToken t WHERE t.queueId = :qId AND t.status IN ('CALLED','IN_SERVICE') ORDER BY t.updatedAt DESC")
    List<QueueToken> findActiveTokens(@Param("qId") Long queueId);

    @Query("SELECT t FROM QueueToken t WHERE t.appointmentId = :apptId")
    Optional<QueueToken> findByAppointmentId(@Param("apptId") Long appointmentId);

    Optional<QueueToken> findByIdAndQueueId(Long id, Long queueId);

    List<QueueToken> findByPatientUserIdOrderByCreatedAtDesc(Long patientUserId);

    Optional<QueueToken> findFirstByQueueIdAndStatusOrderByCreatedAtAsc(Long queueId, TokenStatus status);

    // Pessimistic lock for call-next concurrency control
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM QueueToken t WHERE t.id = :id")
    Optional<QueueToken> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.queueId = :qId AND t.createdAt < :pivot AND t.status IN ('WAITING','CALLED')")
    long countAhead(@Param("qId") Long queueId, @Param("pivot") java.time.Instant pivot);

    List<QueueToken> findByDoctorIdAndQueueId(Long doctorId, Long queueId);

    List<QueueToken> findTop20ByDoctorIdAndStatusAndStartedAtIsNotNullAndCompletedAtIsNotNullOrderByCompletedAtDesc(
            Long doctorId, TokenStatus status);

    List<QueueToken> findByQueueIdAndPatientUserIdIsNotNullAndStatusIn(
            Long queueId, List<TokenStatus> statuses);

    long countByStatus(TokenStatus status);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.status = :status")
    long countByStatus2(@Param("status") TokenStatus status);
}
