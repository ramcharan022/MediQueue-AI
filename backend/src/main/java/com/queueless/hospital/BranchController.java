package com.queueless.hospital;

import com.queueless.common.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BranchController {

    private final HospitalBranchRepository branchRepo;
    private final DepartmentRepository deptRepo;

    public BranchController(HospitalBranchRepository branchRepo, DepartmentRepository deptRepo) {
        this.branchRepo = branchRepo;
        this.deptRepo = deptRepo;
    }

    record BranchResponse(Long id, String name, String address, String city, String timezone, String phone) {}
    record DeptResponse(Long id, Long branchId, String name, String description) {}

    @GetMapping("/branches")
    public List<BranchResponse> listBranches() {
        return branchRepo.findByActiveTrue().stream()
                .map(b -> new BranchResponse(b.getId(), b.getName(), b.getAddress(), b.getCity(), b.getTimezone(), b.getPhone()))
                .toList();
    }

    @GetMapping("/branches/{id}")
    public BranchResponse getBranch(@PathVariable Long id) {
        HospitalBranch b = branchRepo.findById(id)
                .filter(HospitalBranch::isActive)
                .orElseThrow(() -> AppException.notFound("Branch not found"));
        return new BranchResponse(b.getId(), b.getName(), b.getAddress(), b.getCity(), b.getTimezone(), b.getPhone());
    }

    @GetMapping("/branches/{id}/departments")
    public List<DeptResponse> getDepartments(@PathVariable Long id) {
        if (!branchRepo.existsById(id)) throw AppException.notFound("Branch not found");
        return deptRepo.findByBranchIdAndActiveTrue(id).stream()
                .map(d -> new DeptResponse(d.getId(), d.getBranchId(), d.getName(), d.getDescription()))
                .toList();
    }

    @GetMapping("/departments")
    public List<DeptResponse> listAllDepartments() {
        return deptRepo.findByActiveTrue().stream()
                .map(d -> new DeptResponse(d.getId(), d.getBranchId(), d.getName(), d.getDescription()))
                .toList();
    }

    @GetMapping("/departments/{id}")
    public DeptResponse getDepartment(@PathVariable Long id) {
        Department d = deptRepo.findById(id)
                .filter(Department::isActive)
                .orElseThrow(() -> AppException.notFound("Department not found"));
        return new DeptResponse(d.getId(), d.getBranchId(), d.getName(), d.getDescription());
    }
}
