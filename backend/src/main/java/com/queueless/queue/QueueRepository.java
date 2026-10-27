package com.queueless.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {

    Optional<Queue> findByBranchIdAndDepartmentIdAndServiceDate(
            Long branchId, Long departmentId, LocalDate serviceDate);

    @Modifying
    @Query(value = """
            INSERT INTO queue (branch_id, department_id, service_date)
            VALUES (:branchId, :departmentId, :serviceDate)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void insertIfMissing(@Param("branchId") Long branchId,
                         @Param("departmentId") Long departmentId,
                         @Param("serviceDate") LocalDate serviceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Queue q WHERE q.id = :id")
    Optional<Queue> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Queue q WHERE q.branchId = :b AND q.departmentId = :d AND q.serviceDate = :date")
    Optional<Queue> findForUpdate(@Param("b") Long branchId,
                                  @Param("d") Long departmentId,
                                  @Param("date") LocalDate date);
}
