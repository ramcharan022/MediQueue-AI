package com.queueless.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HospitalBranchRepository extends JpaRepository<HospitalBranch, Long> {
    List<HospitalBranch> findByActiveTrue();
}
