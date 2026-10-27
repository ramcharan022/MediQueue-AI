package com.queueless.appointment;

import com.queueless.common.exception.AppException;
import com.queueless.doctor.Doctor;
import com.queueless.doctor.DoctorRepository;
import com.queueless.doctor.AvailabilityService;
import com.queueless.hospital.HospitalBranch;
import com.queueless.hospital.HospitalBranchRepository;
import com.queueless.notification.NotificationService;
import com.queueless.user.PatientProfile;
import com.queueless.user.PatientProfileRepository;
import com.queueless.user.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final AvailabilityService availabilityService;
    private final PatientProfileRepository profileRepository;
    private final NotificationService notificationService;
    private final HospitalBranchRepository branchRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                               DoctorRepository doctorRepository,
                               AvailabilityService availabilityService,
                               PatientProfileRepository profileRepository,
                               HospitalBranchRepository branchRepository,
                               @Lazy NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.availabilityService = availabilityService;
        this.profileRepository = profileRepository;
        this.branchRepository = branchRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Appointment book(User currentUser, BookingRequest req) {
        PatientProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> AppException.badRequest("Patient profile not found."));

        Doctor doctor = doctorRepository.findByIdForUpdate(req.doctorId())
                .filter(Doctor::isActive)
                .orElseThrow(() -> AppException.notFound("Doctor not found"));

        LocalDateTime slot = req.startsAt();
        HospitalBranch branch = branchRepository.findById(doctor.getBranchId())
                .orElseThrow(() -> AppException.notFound("Hospital branch not found."));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(branch.getTimezone()));
        if (slot.isBefore(now)) {
            throw AppException.badRequest("Cannot book a past appointment slot.");
        }
        if (!availabilityService.getAvailableSlots(doctor, slot.toLocalDate()).contains(slot)) {
            throw AppException.conflict("This time is outside the doctor's schedule or is no longer available.");
        }

        // Pessimistic lock: checks for existing booking at same slot
        appointmentRepository.findActiveByDoctorAndSlotForUpdate(doctor.getId(), slot)
                .ifPresent(existing -> { throw AppException.conflict("This time slot is no longer available."); });

        Appointment appt = new Appointment();
        appt.setReference(generateReference(slot));
        appt.setPatientId(profile.getId());
        appt.setDoctorId(doctor.getId());
        appt.setBranchId(doctor.getBranchId());
        appt.setDepartmentId(doctor.getDepartmentId());
        appt.setStartsAt(slot);
        appt.setEndsAt(slot.plusMinutes(doctor.getConsultationMinutes()));
        appt.setNotes(req.notes());

        try {
            appt = appointmentRepository.saveAndFlush(appt);
        } catch (DataIntegrityViolationException e) {
            throw AppException.conflict("This time slot was just booked by someone else.");
        }

        notificationService.createNotification(
                currentUser.getId(), "BOOKING_CONFIRMED",
                "Appointment Confirmed",
                "Your appointment with " + doctor.getFullName() + " on " +
                slot.format(DateTimeFormatter.ofPattern("MMM dd 'at' hh:mm a")) + " is confirmed.",
                appt.getReference());

        return appt;
    }

    @Transactional
    public Appointment cancel(User currentUser, String reference) {
        Appointment appt = appointmentRepository.findByReference(reference)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        PatientProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> AppException.forbidden("Not your appointment"));

        if (!appt.getPatientId().equals(profile.getId())) {
            throw AppException.forbidden("You can only cancel your own appointments.");
        }
        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw AppException.badRequest("Only confirmed appointments can be cancelled.");
        }
        HospitalBranch branch = branchRepository.findById(appt.getBranchId())
                .orElseThrow(() -> AppException.notFound("Hospital branch not found."));
        if (appt.getStartsAt().isBefore(LocalDateTime.now(ZoneId.of(branch.getTimezone())).plusHours(2))) {
            throw AppException.badRequest("Appointments can only be cancelled at least 2 hours in advance.");
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appt);
    }

    public List<Appointment> getMyAppointments(User currentUser) {
        PatientProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> AppException.badRequest("Patient profile not found"));
        return appointmentRepository.findByPatientIdOrderByStartsAtDesc(profile.getId());
    }

    public Appointment getByReference(User currentUser, String reference) {
        Appointment appt = appointmentRepository.findByReference(reference)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));
        PatientProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> AppException.forbidden("Not authorized"));
        if (!appt.getPatientId().equals(profile.getId())) {
            throw AppException.forbidden("You can only view your own appointments.");
        }
        return appt;
    }

    private String generateReference(LocalDateTime slot) {
        String date = slot.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "QL-" + date + "-" + suffix;
    }
}
