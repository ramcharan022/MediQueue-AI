package com.queueless.queue;

import com.queueless.appointment.Appointment;
import com.queueless.appointment.AppointmentRepository;
import com.queueless.appointment.AppointmentStatus;
import com.queueless.audit.AuditService;
import com.queueless.common.exception.AppException;
import com.queueless.doctor.Doctor;
import com.queueless.doctor.DoctorRepository;
import com.queueless.hospital.Department;
import com.queueless.hospital.DepartmentRepository;
import com.queueless.hospital.HospitalBranch;
import com.queueless.hospital.HospitalBranchRepository;
import com.queueless.notification.NotificationService;
import com.queueless.user.PatientProfile;
import com.queueless.user.PatientProfileRepository;
import com.queueless.user.StaffAssignmentRepository;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import com.queueless.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class QueueServiceWorkflowTest {
    @Mock private QueueRepository queueRepository;
    @Mock private QueueTokenRepository tokenRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private QueueEventRepository eventRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientProfileRepository profileRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private HospitalBranchRepository branchRepository;
    @Mock private StaffAssignmentRepository assignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private User admin;
    private QueueService service;

    @BeforeEach
    void setUp() {
        service = new QueueService(queueRepository, tokenRepository, appointmentRepository, eventRepository,
                doctorRepository, profileRepository, userRepository, departmentRepository, branchRepository,
                assignmentRepository, notificationService, auditService, eventPublisher);
        lenient().when(admin.getRole()).thenReturn(UserRole.ADMIN);
        lenient().when(admin.getId()).thenReturn(1L);
    }

    @Test
    void checksInAnEligibleAppointmentAndCreatesAPersistedToken() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        Appointment appointment = mock(Appointment.class);
        Doctor doctor = mock(Doctor.class);
        PatientProfile profile = mock(PatientProfile.class);
        HospitalBranch branch = new HospitalBranch();
        branch.setTimezone("Asia/Kolkata");
        Department department = new Department();
        department.setBranchId(4L);
        Queue queue = mock(Queue.class);
        QueueToken token = mock(QueueToken.class);

        when(appointmentRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(appointment.getBranchId()).thenReturn(4L);
        when(appointment.getDepartmentId()).thenReturn(6L);
        when(appointment.getDoctorId()).thenReturn(8L);
        when(appointment.getPatientId()).thenReturn(10L);
        when(appointment.getStartsAt()).thenReturn(now.minusMinutes(5));
        when(appointment.getEndsAt()).thenReturn(now.plusMinutes(10));
        when(departmentRepository.findById(6L)).thenReturn(Optional.of(department));
        when(doctorRepository.findById(8L)).thenReturn(Optional.of(doctor));
        when(doctor.isActive()).thenReturn(true);
        when(doctor.getId()).thenReturn(8L);
        when(doctor.getConsultationMinutes()).thenReturn(15);
        when(branchRepository.findById(4L)).thenReturn(Optional.of(branch));
        when(tokenRepository.findByAppointmentId(12L)).thenReturn(Optional.empty());
        when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(profile.getUserId()).thenReturn(30L);
        when(queueRepository.findForUpdate(eq(4L), eq(6L), any())).thenReturn(Optional.of(queue));
        when(queue.getId()).thenReturn(99L);
        when(token.getId()).thenReturn(50L);
        when(token.getQueueId()).thenReturn(99L);
        when(token.getDoctorId()).thenReturn(8L);
        when(token.getStatus()).thenReturn(TokenStatus.WAITING);
        when(token.getTokenNumber()).thenReturn("T001");
        when(tokenRepository.saveAndFlush(any(QueueToken.class))).thenReturn(token);
        when(tokenRepository.findById(50L)).thenReturn(Optional.of(token));
        when(tokenRepository.findByQueueIdOrderByCreatedAtAsc(99L)).thenReturn(List.of(token));
        when(tokenRepository.findTop20ByDoctorIdAndStatusAndStartedAtIsNotNullAndCompletedAtIsNotNullOrderByCompletedAtDesc(
                8L, TokenStatus.COMPLETED)).thenReturn(List.of());

        QueueToken checkedIn = service.checkInAppointment(admin, 12L);

        assertSame(token, checkedIn);
        verify(appointment).setStatus(AppointmentStatus.CHECKED_IN);
        verify(appointmentRepository).save(appointment);
        verify(tokenRepository).saveAndFlush(any(QueueToken.class));
        verify(notificationService).createNotification(eq(30L), eq("TOKEN_GENERATED"),
                anyString(), anyString(), anyString());
    }

    @Test
    void refusesCheckInOutsideTheConfiguredWindow() {
        Appointment appointment = mock(Appointment.class);
        Doctor doctor = mock(Doctor.class);
        HospitalBranch branch = new HospitalBranch();
        branch.setTimezone("Asia/Kolkata");
        Department department = new Department();
        department.setBranchId(4L);
        LocalDateTime startsAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(3);

        when(appointmentRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CONFIRMED);
        when(appointment.getBranchId()).thenReturn(4L);
        when(appointment.getDepartmentId()).thenReturn(6L);
        when(appointment.getDoctorId()).thenReturn(8L);
        when(appointment.getStartsAt()).thenReturn(startsAt);
        when(departmentRepository.findById(6L)).thenReturn(Optional.of(department));
        when(doctorRepository.findById(8L)).thenReturn(Optional.of(doctor));
        when(doctor.isActive()).thenReturn(true);
        when(branchRepository.findById(4L)).thenReturn(Optional.of(branch));

        assertThrows(AppException.class, () -> service.checkInAppointment(admin, 12L));

        verify(tokenRepository, never()).saveAndFlush(any());
        verify(appointment, never()).setStatus(AppointmentStatus.CHECKED_IN);
    }

    @Test
    void estimatesDoctorLaneAheadCountAndRemainingConsultationTime() {
        Instant now = Instant.now();
        QueueToken target = mock(QueueToken.class);
        QueueToken waiting = mock(QueueToken.class);
        QueueToken serving = mock(QueueToken.class);
        QueueToken completed = mock(QueueToken.class);
        Doctor doctor = mock(Doctor.class);
        Queue queue = mock(Queue.class);
        HospitalBranch branch = new HospitalBranch();
        branch.setTimezone("Asia/Kolkata");

        when(target.getId()).thenReturn(3L);
        when(target.getDoctorId()).thenReturn(7L);
        when(target.getCreatedAt()).thenReturn(now);
        when(waiting.getId()).thenReturn(1L);
        when(waiting.getDoctorId()).thenReturn(7L);
        when(waiting.getCreatedAt()).thenReturn(now.minusSeconds(300));
        when(waiting.getStatus()).thenReturn(TokenStatus.WAITING);
        when(serving.getId()).thenReturn(2L);
        when(serving.getDoctorId()).thenReturn(7L);
        when(serving.getCreatedAt()).thenReturn(now.minusSeconds(600));
        when(serving.getStatus()).thenReturn(TokenStatus.IN_SERVICE);
        when(serving.getStartedAt()).thenReturn(now.minusSeconds(120));
        when(serving.getTokenNumber()).thenReturn("T002");
        when(doctor.getConsultationMinutes()).thenReturn(15);
        when(doctor.getId()).thenReturn(7L);
        when(completed.getStartedAt()).thenReturn(now.minusSeconds(3600));
        when(completed.getCompletedAt()).thenReturn(now.minusSeconds(2400));
        when(queue.getBranchId()).thenReturn(4L);
        when(tokenRepository.findById(3L)).thenReturn(Optional.of(target));
        when(tokenRepository.findByQueueIdOrderByCreatedAtAsc(9L)).thenReturn(List.of(serving, waiting, target));
        when(tokenRepository.findTop20ByDoctorIdAndStatusAndStartedAtIsNotNullAndCompletedAtIsNotNullOrderByCompletedAtDesc(
                7L, TokenStatus.COMPLETED)).thenReturn(List.of(completed));
        when(doctorRepository.findById(7L)).thenReturn(Optional.of(doctor));
        when(queueRepository.findById(9L)).thenReturn(Optional.of(queue));
        when(branchRepository.findById(4L)).thenReturn(Optional.of(branch));

        QueueService.WaitEstimate estimate = service.estimateWait(9L, 3L);

        assertEquals(2, estimate.patientsAhead());
        assertEquals("T002", estimate.currentlyServingToken());
        assertEquals(33, estimate.lowerMinutes());
        assertEquals(43, estimate.upperMinutes());
        assertEquals("MEDIUM", estimate.confidence());
        assertNotNull(estimate.estimatedArrival());
        assertNotNull(estimate.updatedAt());
    }

    @Test
    void doesNotCallTheSameWaitingTokenTwice() {
        Queue queue = mock(Queue.class);
        QueueToken token = mock(QueueToken.class);
        java.util.concurrent.atomic.AtomicReference<TokenStatus> status =
                new java.util.concurrent.atomic.AtomicReference<>(TokenStatus.WAITING);
        when(queueRepository.findById(9L)).thenReturn(Optional.of(queue));
        when(queueRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(queue));
        when(tokenRepository.findFirstByQueueIdAndStatusOrderByCreatedAtAsc(9L, TokenStatus.WAITING))
                .thenReturn(Optional.of(token));
        when(tokenRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(token));
        when(token.getId()).thenReturn(20L);
        when(token.getQueueId()).thenReturn(9L);
        when(token.getStatus()).thenAnswer(invocation -> status.get());
        doAnswer(invocation -> {
            status.set(invocation.getArgument(0));
            return null;
        }).when(token).setStatus(any(TokenStatus.class));
        when(token.getTokenNumber()).thenReturn("T001");
        when(admin.getId()).thenReturn(1L);

        service.callNext(admin, 9L);

        assertThrows(AppException.class, () -> service.callNext(admin, 9L));

        verify(tokenRepository, times(1)).save(token);
    }

    @Test
    void marksConsultationCompletionTimeAndAppointmentState() {
        Queue queue = mock(Queue.class);
        QueueToken token = mock(QueueToken.class);
        when(queueRepository.findById(9L)).thenReturn(Optional.of(queue));
        when(tokenRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(token));
        when(token.getId()).thenReturn(20L);
        when(token.getQueueId()).thenReturn(9L);
        when(token.getStatus()).thenReturn(TokenStatus.IN_SERVICE);
        when(token.getAppointmentId()).thenReturn(null);
        when(token.getTokenNumber()).thenReturn("T001");
        when(admin.getId()).thenReturn(1L);

        service.completeConsultation(admin, 20L);

        verify(token).setCompletedAt(any(Instant.class));
        verify(tokenRepository).save(token);
    }

    @Test
    void cancelsAWaitingTokenAndItsCheckedInAppointment() {
        Queue queue = mock(Queue.class);
        QueueToken token = mock(QueueToken.class);
        Appointment appointment = mock(Appointment.class);
        when(queueRepository.findById(9L)).thenReturn(Optional.of(queue));
        when(tokenRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(token));
        when(token.getId()).thenReturn(20L);
        when(token.getQueueId()).thenReturn(9L);
        when(token.getStatus()).thenReturn(TokenStatus.WAITING);
        when(token.getAppointmentId()).thenReturn(30L);
        when(token.getTokenNumber()).thenReturn("T001");
        when(appointmentRepository.findById(30L)).thenReturn(Optional.of(appointment));

        service.cancelToken(admin, 20L, "Patient requested cancellation");

        verify(token).setStatus(TokenStatus.CANCELLED);
        verify(appointment).setStatus(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
        verify(auditService).log(1L, "QUEUE_CANCEL", "queue_token", 20L, "Token T001 cancelled");
    }
}
