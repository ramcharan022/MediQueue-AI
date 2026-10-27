package com.queueless.doctor;

import com.queueless.appointment.AppointmentRepository;
import com.queueless.appointment.AppointmentStatus;
import com.queueless.hospital.HospitalBranch;
import com.queueless.hospital.HospitalBranchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private final DoctorScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final HospitalBranchRepository branchRepository;

    public AvailabilityService(DoctorScheduleRepository scheduleRepository,
                                AppointmentRepository appointmentRepository,
                                HospitalBranchRepository branchRepository) {
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Returns available start times for a doctor on a given date.
     * ISO day-of-week: 1=Monday, 7=Sunday
     */
    public List<LocalDateTime> getAvailableSlots(Doctor doctor, LocalDate date) {
        int isoDow = date.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), isoDow);

        if (schedules.isEmpty()) return List.of();

        // Collect booked slots
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<LocalDateTime> booked = appointmentRepository
                .findByDoctorIdAndStartsAtBetweenAndStatusNot(
                        doctor.getId(), dayStart, dayEnd, AppointmentStatus.CANCELLED)
                .stream()
                .map(a -> a.getStartsAt())
                .toList();

        List<LocalDateTime> slots = new ArrayList<>();
        int durationMins = doctor.getConsultationMinutes();
        HospitalBranch branch = branchRepository.findById(doctor.getBranchId())
                .orElseThrow(() -> new IllegalStateException("Doctor is linked to a missing hospital branch."));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(branch.getTimezone()));

        for (DoctorSchedule sched : schedules) {
            LocalTime current = sched.getStartsAt();
            LocalTime end = sched.getEndsAt();
            while (!current.plusMinutes(durationMins).isAfter(end)) {
                LocalDateTime slot = LocalDateTime.of(date, current);
                if (!booked.contains(slot) && slot.isAfter(now)) {
                    slots.add(slot);
                }
                current = current.plusMinutes(durationMins);
            }
        }
        return slots;
    }
}
