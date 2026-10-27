package com.queueless.waitinglist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, Long> {
    List<WaitingListEntry> findByPatientIdOrderByRequestedAtDesc(Long patientId);
    Optional<WaitingListEntry> findByIdAndPatientId(Long id, Long patientId);
    boolean existsByPatientIdAndDepartmentIdAndDoctorIdAndStatus(
            Long patientId, Long departmentId, Long doctorId, WaitingListStatus status);
    List<WaitingListEntry> findByDepartmentIdAndStatusOrderByRequestedAtAsc(
            Long departmentId, WaitingListStatus status);
    Optional<WaitingListEntry> findByIdAndDepartmentId(Long id, Long departmentId);
}
