package com.queueless.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByReference(String reference);

    List<Appointment> findByPatientIdOrderByStartsAtDesc(Long patientId);

    List<Appointment> findByBranchIdAndDepartmentIdAndStartsAtGreaterThanEqualAndStartsAtLessThanAndStatusOrderByStartsAtAsc(
            Long branchId, Long departmentId, LocalDateTime from, LocalDateTime to, AppointmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    List<Appointment> findByDoctorIdAndStartsAtBetweenAndStatusNot(
            Long doctorId, LocalDateTime from, LocalDateTime to, AppointmentStatus excludeStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.startsAt = :startsAt AND a.status <> 'CANCELLED'")
    Optional<Appointment> findActiveByDoctorAndSlotForUpdate(@Param("doctorId") Long doctorId,
                                                              @Param("startsAt") LocalDateTime startsAt);

    long countByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    long countByStatus(AppointmentStatus status);

    List<Appointment> findAllByOrderByStartsAtDesc();
}
