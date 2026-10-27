package com.queueless.admin;

import com.queueless.analytics.AnalyticsService;
import com.queueless.appointment.Appointment;
import com.queueless.appointment.AppointmentRepository;
import com.queueless.audit.AuditLog;
import com.queueless.audit.AuditLogRepository;
import com.queueless.doctor.Doctor;
import com.queueless.doctor.DoctorRepository;
import com.queueless.doctor.DoctorSchedule;
import com.queueless.doctor.DoctorScheduleRepository;
import com.queueless.hospital.Department;
import com.queueless.hospital.DepartmentRepository;
import com.queueless.hospital.HospitalBranch;
import com.queueless.hospital.HospitalBranchRepository;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import com.queueless.user.UserRole;
import com.queueless.user.StaffAssignment;
import com.queueless.user.StaffAssignmentRepository;
import com.queueless.common.exception.AppException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final HospitalBranchRepository branchRepo;
    private final DepartmentRepository deptRepo;
    private final DoctorRepository doctorRepo;
    private final DoctorScheduleRepository scheduleRepo;
    private final UserRepository userRepo;
    private final AppointmentRepository appointmentRepo;
    private final AuditLogRepository auditRepo;
    private final AnalyticsService analyticsService;
    private final PasswordEncoder passwordEncoder;
    private final StaffAssignmentRepository staffAssignmentRepo;

    public AdminController(HospitalBranchRepository branchRepo,
                           DepartmentRepository deptRepo,
                           DoctorRepository doctorRepo,
                           DoctorScheduleRepository scheduleRepo,
                           UserRepository userRepo,
                           AppointmentRepository appointmentRepo,
                           AuditLogRepository auditRepo,
                           AnalyticsService analyticsService,
                           PasswordEncoder passwordEncoder,
                           StaffAssignmentRepository staffAssignmentRepo) {
        this.branchRepo = branchRepo;
        this.deptRepo = deptRepo;
        this.doctorRepo = doctorRepo;
        this.scheduleRepo = scheduleRepo;
        this.userRepo = userRepo;
        this.appointmentRepo = appointmentRepo;
        this.auditRepo = auditRepo;
        this.analyticsService = analyticsService;
        this.passwordEncoder = passwordEncoder;
        this.staffAssignmentRepo = staffAssignmentRepo;
    }

    // ── Analytics ──────────────────────────────────────────

    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        return analyticsService.getSummaryStats();
    }

    // ── Audit Logs ──────────────────────────────────────────

    @GetMapping("/audit-logs")
    public Page<AuditLogResponse> auditLogs(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return auditRepo.findAllByOrderByOccurredAtDesc(
                PageRequest.of(page, Math.min(size, 200)))
                .map(l -> new AuditLogResponse(l.getId(), l.getActorUserId(), l.getAction(),
                        l.getTargetType(), l.getTargetId(), l.getDetail(), l.getOccurredAt().toString()));
    }

    // ── Appointments ──────────────────────────────────────────

    @GetMapping("/appointments")
    public List<AppointmentSummary> allAppointments() {
        return appointmentRepo.findAllByOrderByStartsAtDesc().stream()
                .map(a -> new AppointmentSummary(a.getId(), a.getReference(), a.getPatientId(),
                        a.getDoctorId(), a.getDepartmentId(), a.getBranchId(),
                        a.getStatus().name(), a.getStartsAt().toString()))
                .toList();
    }

    // ── Branch CRUD ──────────────────────────────────────────

    @GetMapping("/branches")
    public List<HospitalBranch> branches() { return branchRepo.findAll(); }

    @PostMapping("/branches")
    public HospitalBranch createBranch(@RequestBody BranchRequest req) {
        HospitalBranch b = new HospitalBranch();
        b.setName(req.name()); b.setAddress(req.address());
        b.setCity(req.city()); b.setTimezone(req.timezone()); b.setPhone(req.phone());
        return branchRepo.save(b);
    }

    @PutMapping("/branches/{id}")
    public HospitalBranch updateBranch(@PathVariable Long id, @RequestBody BranchRequest req) {
        HospitalBranch b = branchRepo.findById(id).orElseThrow(() -> AppException.notFound("Branch not found"));
        b.setName(req.name()); b.setAddress(req.address());
        b.setCity(req.city()); b.setTimezone(req.timezone()); b.setPhone(req.phone());
        return branchRepo.save(b);
    }

    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchRepo.findById(id).orElseThrow(() -> AppException.notFound("Branch not found"));
        branchRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Department CRUD ──────────────────────────────────────────

    @GetMapping("/departments")
    public List<Department> departments() { return deptRepo.findAll(); }

    @PostMapping("/departments")
    public Department createDepartment(@RequestBody DeptRequest req) {
        branchRepo.findById(req.branchId()).orElseThrow(() -> AppException.notFound("Branch not found"));
        Department d = new Department();
        d.setBranchId(req.branchId()); d.setName(req.name()); d.setDescription(req.description());
        return deptRepo.save(d);
    }

    @PutMapping("/departments/{id}")
    public Department updateDepartment(@PathVariable Long id, @RequestBody DeptRequest req) {
        Department d = deptRepo.findById(id).orElseThrow(() -> AppException.notFound("Department not found"));
        branchRepo.findById(req.branchId()).orElseThrow(() -> AppException.notFound("Branch not found"));
        d.setBranchId(req.branchId()); d.setName(req.name()); d.setDescription(req.description());
        return deptRepo.save(d);
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        deptRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Doctor CRUD ──────────────────────────────────────────

    @GetMapping("/doctors")
    public List<DoctorResponse> doctors() {
        return doctorRepo.findAll().stream()
                .map(d -> new DoctorResponse(d.getId(), d.getBranchId(), d.getDepartmentId(),
                        d.getFullName(), d.getSpecialization(), d.getQualification(),
                        d.getConsultationMinutes(), d.isActive()))
                .toList();
    }

    @PostMapping("/doctors")
    public DoctorResponse createDoctor(@RequestBody DoctorRequest req) {
        validateDoctorLocation(req);
        Doctor d = new Doctor();
        d.setBranchId(req.branchId()); d.setDepartmentId(req.departmentId());
        d.setFullName(req.fullName()); d.setSpecialization(req.specialization());
        d.setQualification(req.qualification()); d.setConsultationMinutes(req.consultationMinutes());
        d = doctorRepo.save(d);
        return toDoctorResponse(d);
    }

    @PutMapping("/doctors/{id}")
    public DoctorResponse updateDoctor(@PathVariable Long id, @RequestBody DoctorRequest req) {
        validateDoctorLocation(req);
        Doctor d = doctorRepo.findById(id).orElseThrow(() -> AppException.notFound("Doctor not found"));
        d.setBranchId(req.branchId()); d.setDepartmentId(req.departmentId());
        d.setFullName(req.fullName()); d.setSpecialization(req.specialization());
        d.setQualification(req.qualification()); d.setConsultationMinutes(req.consultationMinutes());
        d.setActive(req.active());
        return toDoctorResponse(doctorRepo.save(d));
    }

    private void validateDoctorLocation(DoctorRequest req) {
        branchRepo.findById(req.branchId()).orElseThrow(() -> AppException.notFound("Branch not found"));
        Department department = deptRepo.findById(req.departmentId())
                .orElseThrow(() -> AppException.notFound("Department not found"));
        if (!department.getBranchId().equals(req.branchId())) {
            throw AppException.badRequest("The selected department does not belong to this branch.");
        }
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Staff Management ──────────────────────────────────────────

    @GetMapping("/staff")
    public List<StaffResponse> listStaff() {
        return userRepo.findByRole(UserRole.STAFF).stream()
                .map(u -> {
                    StaffAssignment assignment = staffAssignmentRepo.findByUserId(u.getId()).orElse(null);
                    return new StaffResponse(u.getId(), u.getEmail(), u.getRole().name(), u.isActive(),
                            u.getCreatedAt().toString(), assignment == null ? null : assignment.getBranchId(),
                            assignment == null ? null : assignment.getDepartmentId());
                })
                .toList();
    }

    @PostMapping("/staff")
    @Transactional
    public StaffResponse createStaff(@Valid @RequestBody StaffRequest req) {
        if (userRepo.existsByEmail(req.email())) throw AppException.conflict("Email already registered");
        validateStaffAssignment(req.branchId(), req.departmentId());
        User u = new User(req.email().toLowerCase().trim(),
                passwordEncoder.encode(req.password()), UserRole.STAFF);
        u = userRepo.save(u);
        StaffAssignment assignment = new StaffAssignment();
        assignment.setUserId(u.getId());
        assignment.setBranchId(req.branchId());
        assignment.setDepartmentId(req.departmentId());
        staffAssignmentRepo.save(assignment);
        return staffResponse(u, assignment);
    }

    @PutMapping("/staff/{id}")
    @Transactional
    public StaffResponse updateStaff(@PathVariable Long id, @Valid @RequestBody StaffUpdateRequest req) {
        User u = userRepo.findById(id).orElseThrow(() -> AppException.notFound("Staff not found"));
        if (u.getRole() != UserRole.STAFF) throw AppException.badRequest("Only staff accounts can be updated here.");
        if (req.active() != null) u.setActive(req.active());
        u = userRepo.save(u);
        StaffAssignment assignment = staffAssignmentRepo.findByUserId(id).orElseGet(StaffAssignment::new);
        if (req.branchId() != null || req.departmentId() != null) {
            validateStaffAssignment(req.branchId(), req.departmentId());
            assignment.setUserId(u.getId());
            assignment.setBranchId(req.branchId());
            assignment.setDepartmentId(req.departmentId());
            assignment = staffAssignmentRepo.save(assignment);
        }
        return staffResponse(u, assignment);
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> AppException.notFound("Staff not found"));
        if (user.getRole() != UserRole.STAFF) throw AppException.badRequest("Only staff accounts can be deleted here.");
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──────────────────────────────────────────

    record BranchRequest(String name, String address, String city, String timezone, String phone) {}
    record DeptRequest(Long branchId, String name, String description) {}
    record DoctorRequest(Long branchId, Long departmentId, String fullName, String specialization,
                          String qualification, int consultationMinutes, boolean active) {}
    record StaffRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email String email,
                        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 12, max = 72) String password,
                        @jakarta.validation.constraints.NotNull Long branchId,
                        @jakarta.validation.constraints.NotNull Long departmentId) {}
    record StaffUpdateRequest(Boolean active, Long branchId, Long departmentId) {}
    record StaffResponse(Long id, String email, String role, boolean active, String createdAt,
                         Long branchId, Long departmentId) {}
    record DoctorResponse(Long id, Long branchId, Long departmentId, String fullName,
                           String specialization, String qualification, int consultationMinutes, boolean active) {}
    record AuditLogResponse(Long id, Long actorUserId, String action, String targetType,
                             Long targetId, String detail, String occurredAt) {}
    record AppointmentSummary(Long id, String reference, Long patientId, Long doctorId,
                               Long departmentId, Long branchId, String status, String startsAt) {}

    private DoctorResponse toDoctorResponse(Doctor d) {
        return new DoctorResponse(d.getId(), d.getBranchId(), d.getDepartmentId(),
                d.getFullName(), d.getSpecialization(), d.getQualification(),
                d.getConsultationMinutes(), d.isActive());
    }

    private void validateStaffAssignment(Long branchId, Long departmentId) {
        if (branchId == null || departmentId == null) {
            throw AppException.badRequest("A staff assignment requires both a branch and department.");
        }
        branchRepo.findById(branchId).orElseThrow(() -> AppException.notFound("Branch not found."));
        Department department = deptRepo.findById(departmentId)
                .orElseThrow(() -> AppException.notFound("Department not found."));
        if (!branchId.equals(department.getBranchId())) {
            throw AppException.badRequest("The selected department does not belong to this branch.");
        }
    }

    private StaffResponse staffResponse(User user, StaffAssignment assignment) {
        return new StaffResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isActive(),
                user.getCreatedAt().toString(), assignment.getBranchId(), assignment.getDepartmentId());
    }
}
