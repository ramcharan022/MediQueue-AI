package com.queueless.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByBranchIdAndActiveTrue(Long branchId);
    List<Department> findByActiveTrue();
}
