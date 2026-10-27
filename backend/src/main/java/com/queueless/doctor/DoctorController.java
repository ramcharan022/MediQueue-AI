package com.queueless.doctor;

import com.queueless.common.exception.AppException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepo;
    private final DoctorScheduleRepository scheduleRepo;
    private final AvailabilityService availabilityService;

    public DoctorController(DoctorRepository doctorRepo,
                            DoctorScheduleRepository scheduleRepo,
                            AvailabilityService availabilityService) {
        this.doctorRepo = doctorRepo;
        this.scheduleRepo = scheduleRepo;
        this.availabilityService = availabilityService;
    }

    record ScheduleInfo(int dayOfWeek, String startsAt, String endsAt) {}
    record DoctorResponse(Long id, Long branchId, Long departmentId, String fullName,
                          String specialization, String qualification, int consultationMinutes,
                          List<ScheduleInfo> schedules) {}

    private DoctorResponse toResponse(Doctor d) {
        List<ScheduleInfo> schedules = scheduleRepo.findByDoctorId(d.getId()).stream()
                .map(s -> new ScheduleInfo(s.getDayOfWeek(), s.getStartsAt().toString(), s.getEndsAt().toString()))
                .toList();
        return new DoctorResponse(d.getId(), d.getBranchId(), d.getDepartmentId(),
                d.getFullName(), d.getSpecialization(), d.getQualification(),
                d.getConsultationMinutes(), schedules);
    }

    @GetMapping
    public List<DoctorResponse> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long departmentId) {
        List<Doctor> doctors;
        if (branchId != null && departmentId != null) {
            doctors = doctorRepo.findByBranchIdAndDepartmentIdAndActiveTrue(branchId, departmentId);
        } else if (departmentId != null) {
            doctors = doctorRepo.findByDepartmentIdAndActiveTrue(departmentId);
        } else {
            doctors = doctorRepo.findByActiveTrue();
        }
        return doctors.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable Long id) {
        Doctor d = doctorRepo.findById(id)
                .filter(Doctor::isActive)
                .orElseThrow(() -> AppException.notFound("Doctor not found"));
        return toResponse(d);
    }

    @GetMapping("/{id}/availability")
    public Map<String, Object> availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Doctor d = doctorRepo.findById(id)
                .filter(Doctor::isActive)
                .orElseThrow(() -> AppException.notFound("Doctor not found"));
        List<LocalDateTime> slots = availabilityService.getAvailableSlots(d, date);
        return Map.of("doctorId", id, "date", date.toString(), "slots", slots);
    }
}
