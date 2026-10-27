package com.queueless.appointment;

import com.queueless.common.exception.AppException;
import com.queueless.doctor.AvailabilityService;
import com.queueless.doctor.Doctor;
import com.queueless.doctor.DoctorRepository;
import com.queueless.hospital.HospitalBranch;
import com.queueless.hospital.HospitalBranchRepository;
import com.queueless.notification.NotificationService;
import com.queueless.user.PatientProfile;
import com.queueless.user.PatientProfileRepository;
import com.queueless.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private PatientProfileRepository profileRepository;
    @Mock private HospitalBranchRepository branchRepository;
    @Mock private NotificationService notificationService;
    @Mock private User patient;
    @Mock private PatientProfile profile;
    @Mock private Doctor doctor;
    @Mock private HospitalBranch branch;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, doctorRepository, availabilityService,
                profileRepository, branchRepository, notificationService);
        lenient().when(patient.getId()).thenReturn(10L);
        lenient().when(profile.getId()).thenReturn(20L);
        lenient().when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        lenient().when(doctorRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(doctor));
        lenient().when(doctor.isActive()).thenReturn(true);
        lenient().when(doctor.getId()).thenReturn(5L);
        lenient().when(doctor.getBranchId()).thenReturn(2L);
        lenient().when(doctor.getDepartmentId()).thenReturn(3L);
        lenient().when(doctor.getFullName()).thenReturn("Dr. Example");
        lenient().when(doctor.getConsultationMinutes()).thenReturn(15);
        lenient().when(branch.getTimezone()).thenReturn("Asia/Kolkata");
        lenient().when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
    }

    @Test
    void booksAnAvailableSlotAndReturnsItsReference() {
        LocalDateTime slot = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(2);
        when(availabilityService.getAvailableSlots(doctor, slot.toLocalDate())).thenReturn(List.of(slot));
        when(appointmentRepository.findActiveByDoctorAndSlotForUpdate(5L, slot)).thenReturn(Optional.empty());
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment appointment = service.book(patient, new BookingRequest(5L, slot, "Annual checkup"));

        assertTrue(appointment.getReference().startsWith("QL-"));
        assertEquals(20L, appointment.getPatientId());
        assertEquals(5L, appointment.getDoctorId());
        assertEquals(slot.plusMinutes(15), appointment.getEndsAt());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        verify(notificationService).createNotification(eq(10L), eq("BOOKING_CONFIRMED"),
                anyString(), anyString(), eq(appointment.getReference()));
    }

    @Test
    void rejectsASlotOutsideAvailability() {
        LocalDateTime slot = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(2);
        when(availabilityService.getAvailableSlots(doctor, slot.toLocalDate())).thenReturn(List.of());

        assertThrows(AppException.class, () -> service.book(patient, new BookingRequest(5L, slot, null)));

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnAlreadyBookedSlotBeforePersisting() {
        LocalDateTime slot = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(2);
        when(availabilityService.getAvailableSlots(doctor, slot.toLocalDate())).thenReturn(List.of(slot));
        when(appointmentRepository.findActiveByDoctorAndSlotForUpdate(5L, slot))
                .thenReturn(Optional.of(new Appointment()));

        assertThrows(AppException.class, () -> service.book(patient, new BookingRequest(5L, slot, null)));

        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCancellationLessThanTwoHoursBeforeTheAppointment() {
        Appointment appointment = new Appointment();
        appointment.setReference("QL-REFERENCE");
        appointment.setPatientId(20L);
        appointment.setBranchId(2L);
        appointment.setStartsAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(1));
        when(appointmentRepository.findByReference("QL-REFERENCE")).thenReturn(Optional.of(appointment));

        assertThrows(AppException.class, () -> service.cancel(patient, "QL-REFERENCE"));

        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void cancelsAnOwnedAppointmentWhenThePolicyAllowsIt() {
        Appointment appointment = new Appointment();
        appointment.setReference("QL-REFERENCE");
        appointment.setPatientId(20L);
        appointment.setBranchId(2L);
        appointment.setStartsAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(3));
        when(appointmentRepository.findByReference("QL-REFERENCE")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        Appointment cancelled = service.cancel(patient, "QL-REFERENCE");

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
    }
}
