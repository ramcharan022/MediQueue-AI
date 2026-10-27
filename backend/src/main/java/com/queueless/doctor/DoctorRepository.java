package com.queueless.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    List<Doctor> findByBranchIdAndDepartmentIdAndActiveTrue(Long branchId, Long departmentId);
    List<Doctor> findByDepartmentIdAndActiveTrue(Long departmentId);
    List<Doctor> findByActiveTrue();
    List<Doctor> findByBranchId(Long branchId);
    long countByActiveTrue();
}
