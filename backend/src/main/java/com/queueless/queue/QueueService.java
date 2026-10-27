package com.queueless.queue;

import com.queueless.audit.AuditService;
import com.queueless.appointment.Appointment;
import com.queueless.appointment.AppointmentRepository;
import com.queueless.appointment.AppointmentStatus;
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
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import com.queueless.user.StaffAssignment;
import com.queueless.user.StaffAssignmentRepository;
import com.queueless.user.UserRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Core queue management service.
 *
 * Queue-row and token-row locks serialize token allocation, call-next selection,
 * and concurrent state changes within each persisted service queue.
 */
@Service
public class QueueService {

    private final QueueRepository queueRepo;
    private final QueueTokenRepository tokenRepo;
    private final AppointmentRepository appointmentRepo;
    private final QueueEventRepository eventRepo;
    private final DoctorRepository doctorRepo;
    private final PatientProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final HospitalBranchRepository branchRepo;
    private final StaffAssignmentRepository staffAssignmentRepo;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public QueueService(QueueRepository queueRepo,
                        QueueTokenRepository tokenRepo,
                        AppointmentRepository appointmentRepo,
                        QueueEventRepository eventRepo,
                        DoctorRepository doctorRepo,
                        PatientProfileRepository profileRepo,
                        UserRepository userRepo,
                        DepartmentRepository departmentRepo,
                        HospitalBranchRepository branchRepo,
                        StaffAssignmentRepository staffAssignmentRepo,
                        NotificationService notificationService,
                        AuditService auditService,
                        ApplicationEventPublisher eventPublisher) {
        this.queueRepo = queueRepo;
        this.tokenRepo = tokenRepo;
        this.appointmentRepo = appointmentRepo;
        this.eventRepo = eventRepo;
        this.doctorRepo = doctorRepo;
        this.profileRepo = profileRepo;
        this.userRepo = userRepo;
        this.departmentRepo = departmentRepo;
        this.branchRepo = branchRepo;
        this.staffAssignmentRepo = staffAssignmentRepo;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    // ──────────────────────────────────────────────
    // Queue resolution (creates queue for today if not present)
    // ──────────────────────────────────────────────

    @Transactional
    public Queue getOrCreateQueue(Long branchId, Long departmentId) {
        HospitalBranch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> AppException.notFound("Hospital branch not found."));
        return getOrCreateQueue(branchId, departmentId, LocalDate.now(ZoneId.of(branch.getTimezone())));
    }

    private Queue getOrCreateQueue(Long branchId, Long departmentId, LocalDate serviceDate) {
        queueRepo.insertIfMissing(branchId, departmentId, serviceDate);
        return queueRepo.findForUpdate(branchId, departmentId, serviceDate)
                .orElseThrow(() -> AppException.conflict("Could not open the queue."));
    }

    public Queue getQueue(Long queueId) {
        return queueRepo.findById(queueId)
                .orElseThrow(() -> AppException.notFound("Queue not found"));
    }

    public void assertStaffCanAccessDepartment(User staff, Long departmentId) {
        if (staff.getRole() == UserRole.ADMIN) return;
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> AppException.notFound("Department not found."));
        StaffAssignment assignment = staffAssignmentRepo.findByUserId(staff.getId())
                .orElseThrow(() -> AppException.forbidden("Your account is not assigned to a hospital department."));
        if (!assignment.getDepartmentId().equals(departmentId)
                || !assignment.getBranchId().equals(department.getBranchId())) {
            throw AppException.forbidden("You are not assigned to this hospital department.");
        }
    }

    public void assertStaffCanAccessQueue(User staff, Long queueId) {
        Queue queue = getQueue(queueId);
        assertStaffCanAccessDepartment(staff, queue.getDepartmentId());
        if (staff.getRole() != UserRole.ADMIN) {
            StaffAssignment assignment = staffAssignmentRepo.findByUserId(staff.getId())
                    .orElseThrow(() -> AppException.forbidden("Your account is not assigned to this queue."));
            if (!assignment.getBranchId().equals(queue.getBranchId())) {
                throw AppException.forbidden("You are not assigned to this hospital branch.");
            }
        }
    }

    public void assertStaffCanAccessQueueScope(User staff, Long branchId, Long departmentId) {
        assertStaffCanAccessDepartment(staff, departmentId);
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> AppException.notFound("Department not found."));
        if (!department.getBranchId().equals(branchId)) {
            throw AppException.badRequest("The selected department does not belong to this branch.");
        }
    }

    // ──────────────────────────────────────────────
    // Join queue (patient)
    // ──────────────────────────────────────────────

    @Transactional
    public QueueToken joinQueue(User patient, Long branchId, Long departmentId,
                                 Long doctorId, Long appointmentId) {
        PatientProfile profile = profileRepo.findByUserId(patient.getId())
                .orElseThrow(() -> AppException.badRequest("Patient profile not found."));
        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepo.findByIdForUpdate(appointmentId)
                    .orElseThrow(() -> AppException.notFound("Appointment not found."));
            if (!appointment.getPatientId().equals(profile.getId())) {
                throw AppException.forbidden("You can only join the queue for your own appointment.");
            }
            if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
                throw AppException.badRequest("An appointment must be checked in by hospital staff before joining the queue.");
            }
            if (!appointment.getBranchId().equals(branchId)
                    || !appointment.getDepartmentId().equals(departmentId)
                    || (doctorId != null && !appointment.getDoctorId().equals(doctorId))) {
                throw AppException.badRequest("Queue details must match the appointment.");
            }
            branchId = appointment.getBranchId();
            departmentId = appointment.getDepartmentId();
            doctorId = appointment.getDoctorId();
        }
        LocalDate serviceDate = appointment == null
                ? LocalDate.now(ZoneId.of(branchRepo.findById(branchId)
                        .orElseThrow(() -> AppException.notFound("Hospital branch not found."))
                        .getTimezone()))
                : appointment.getStartsAt().toLocalDate();
        Queue queue = getOrCreateQueue(branchId, departmentId, serviceDate);

        // Prevent duplicate join
        if (appointmentId != null) {
            Optional<QueueToken> existing = tokenRepo.findByAppointmentId(appointmentId);
            if (existing.isPresent()) {
                QueueToken t = existing.get();
                if (t.getStatus() != TokenStatus.CANCELLED) {
                    throw AppException.conflict("Already in queue. Token: " + t.getTokenNumber());
                }
            }
        }

        Doctor doctor = null;
        if (doctorId != null) {
            doctor = doctorRepo.findById(doctorId)
                    .filter(Doctor::isActive)
                    .orElseThrow(() -> AppException.notFound("Doctor not found or unavailable"));
        }

        String tokenNumber = generateTokenNumber(queue);

        QueueToken token = new QueueToken();
        token.setQueueId(queue.getId());
        token.setPatientUserId(patient.getId());
        token.setAppointmentId(appointmentId);
        token.setDoctorId(doctor != null ? doctor.getId() : null);
        token.setTokenNumber(tokenNumber);
        token.setAllocationReason(doctor != null
                ? "Patient selected Dr. " + doctor.getFullName()
                : "Walk-in — doctor to be assigned by staff");

        token = tokenRepo.save(token);
        recordEvent(token, null, TokenStatus.WAITING, "Patient joined queue");

        // Estimate initial wait
        int estimatedWait = estimateWaitMinutes(queue.getId(), token.getId());
        token.setEstimatedWaitMinutes(estimatedWait);
        tokenRepo.save(token);
        eventPublisher.publishEvent(QueueUpdateEvent.from(token));

        // Notify patient
        notificationService.createNotification(
                patient.getId(), "TOKEN_GENERATED",
                "Queue Token Generated",
                "Your token is " + tokenNumber + ". Estimated wait: " + estimatedWait + " minutes.",
                tokenNumber);

        return token;
    }

    @Transactional(readOnly = true)
    public List<Appointment> getCheckInAppointments(User staff, Long branchId,
                                                     Long departmentId, LocalDate date) {
        assertStaffCanAccessQueueScope(staff, branchId, departmentId);
        return appointmentRepo.findByBranchIdAndDepartmentIdAndStartsAtGreaterThanEqualAndStartsAtLessThanAndStatusOrderByStartsAtAsc(
                branchId, departmentId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                AppointmentStatus.CONFIRMED);
    }

    @Transactional
    public QueueToken checkInAppointment(User staff, Long appointmentId) {
        Appointment appointment = appointmentRepo.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found."));
        assertStaffCanAccessQueueScope(staff, appointment.getBranchId(), appointment.getDepartmentId());
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw AppException.conflict("Only a confirmed appointment can be checked in.");
        }

        Doctor doctor = doctorRepo.findById(appointment.getDoctorId())
                .filter(Doctor::isActive)
                .orElseThrow(() -> AppException.conflict("This doctor is currently unavailable."));
        HospitalBranch branch = branchRepo.findById(appointment.getBranchId())
                .orElseThrow(() -> AppException.notFound("Hospital branch not found."));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(branch.getTimezone()));
        LocalDateTime checkInOpensAt = appointment.getStartsAt().minusMinutes(60);
        if (now.isBefore(checkInOpensAt) || now.isAfter(appointment.getEndsAt())) {
            throw AppException.badRequest("Check-in is available from 60 minutes before the appointment until its scheduled end.");
        }

        if (tokenRepo.findByAppointmentId(appointmentId).filter(t -> t.getStatus() != TokenStatus.CANCELLED).isPresent()) {
            throw AppException.conflict("This appointment already has a queue token.");
        }
        PatientProfile profile = profileRepo.findById(appointment.getPatientId())
                .orElseThrow(() -> AppException.notFound("Patient profile not found."));

        Queue queue = getOrCreateQueue(appointment.getBranchId(), appointment.getDepartmentId(),
                appointment.getStartsAt().toLocalDate());
        QueueToken token = createToken(queue, doctor, profile.getUserId(), appointment.getId(),
                "Checked in by hospital staff");
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointmentRepo.save(appointment);
        return token;
    }

    private QueueToken createToken(Queue queue, Doctor doctor, Long patientUserId,
                                   Long appointmentId, String reason) {
        QueueToken token = new QueueToken();
        token.setQueueId(queue.getId());
        token.setPatientUserId(patientUserId);
        token.setAppointmentId(appointmentId);
        token.setDoctorId(doctor.getId());
        token.setTokenNumber(generateTokenNumber(queue));
        token.setAllocationReason(reason);
        token = tokenRepo.saveAndFlush(token);
        recordEvent(token, null, null, reason);
        int estimatedWait = estimateWait(queue.getId(), token.getId()).upperMinutes();
        token.setEstimatedWaitMinutes(estimatedWait);
        tokenRepo.save(token);
        eventPublisher.publishEvent(QueueUpdateEvent.from(token));
        notificationService.createNotification(patientUserId, "TOKEN_GENERATED",
                "Queue Token Generated",
                "Your token is " + token.getTokenNumber() + ". Your estimated wait is approximately " +
                        estimatedWait + " minutes and may change.",
                token.getTokenNumber());
        return token;
    }

    // ──────────────────────────────────────────────
    // Staff actions
    // ──────────────────────────────────────────────

    /**
     * Atomically calls the next WAITING token in the queue.
     * Uses pessimistic lock to prevent duplicate assignments.
     */
    @Transactional
    public QueueToken callNext(User staff, Long queueId) {
        assertStaffCanAccessQueue(staff, queueId);
        queueRepo.findByIdForUpdate(queueId)
                .orElseThrow(() -> AppException.notFound("Queue not found."));
        // Find next waiting token (ordered by creation time)
        QueueToken next = tokenRepo.findFirstByQueueIdAndStatusOrderByCreatedAtAsc(queueId, TokenStatus.WAITING)
                .orElseThrow(() -> AppException.notFound("No waiting patients in queue"));

        // Acquire pessimistic lock before transitioning
        QueueToken locked = tokenRepo.findByIdForUpdate(next.getId())
                .orElseThrow(() -> AppException.notFound("Token no longer available"));

        if (locked.getStatus() != TokenStatus.WAITING) {
            throw AppException.conflict("Token was already processed concurrently");
        }

        transition(locked, staff.getId(), TokenStatus.CALLED, "Called by staff");

        // Notify the patient
        notifyTokenCalled(locked, staff);

        auditService.log(staff.getId(), "QUEUE_CALL_NEXT", "queue_token",
                locked.getId(), "Token " + locked.getTokenNumber() + " called");

        return locked;
    }

    @Transactional
    public QueueToken startConsultation(User staff, Long tokenId) {
        QueueToken token = getLockedToken(tokenId);
        assertStaffCanAccessQueue(staff, token.getQueueId());
        transition(token, staff.getId(), TokenStatus.IN_SERVICE, "Consultation started");
        auditService.log(staff.getId(), "QUEUE_START_CONSULTATION", "queue_token",
                tokenId, "Token " + token.getTokenNumber());
        return token;
    }

    @Transactional
    public QueueToken completeConsultation(User staff, Long tokenId) {
        QueueToken token = getLockedToken(tokenId);
        assertStaffCanAccessQueue(staff, token.getQueueId());
        transition(token, staff.getId(), TokenStatus.COMPLETED, "Consultation completed");
        if (token.getAppointmentId() != null) {
            appointmentRepo.findById(token.getAppointmentId()).ifPresent(appointment -> {
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointmentRepo.save(appointment);
            });
        }
        auditService.log(staff.getId(), "QUEUE_COMPLETE_CONSULTATION", "queue_token",
                tokenId, "Token " + token.getTokenNumber());
        return token;
    }

    @Transactional
    public QueueToken skipToken(User staff, Long tokenId, String reason) {
        QueueToken token = getLockedToken(tokenId);
        assertStaffCanAccessQueue(staff, token.getQueueId());
        transition(token, staff.getId(), TokenStatus.SKIPPED,
                reason != null ? reason : "Skipped by staff");
        auditService.log(staff.getId(), "QUEUE_SKIP", "queue_token",
                tokenId, "Token " + token.getTokenNumber() + ": " + reason);
        return token;
    }

    @Transactional
    public QueueToken markNoShow(User staff, Long tokenId) {
        QueueToken token = getLockedToken(tokenId);
        assertStaffCanAccessQueue(staff, token.getQueueId());
        transition(token, staff.getId(), TokenStatus.NO_SHOW, "Marked as no-show");
        if (token.getAppointmentId() != null) {
            appointmentRepo.findById(token.getAppointmentId()).ifPresent(appointment -> {
                appointment.setStatus(AppointmentStatus.NO_SHOW);
                appointmentRepo.save(appointment);
            });
        }
        auditService.log(staff.getId(), "QUEUE_NO_SHOW", "queue_token",
                tokenId, "Token " + token.getTokenNumber());
        return token;
    }

    @Transactional
    public QueueToken cancelToken(User staff, Long tokenId, String reason) {
        QueueToken token = getLockedToken(tokenId);
        assertStaffCanAccessQueue(staff, token.getQueueId());
        transition(token, staff.getId(), TokenStatus.CANCELLED,
                reason == null || reason.isBlank() ? "Cancelled by staff" : reason);
        if (token.getAppointmentId() != null) {
            appointmentRepo.findById(token.getAppointmentId()).ifPresent(appointment -> {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepo.save(appointment);
            });
        }
        auditService.log(staff.getId(), "QUEUE_CANCEL", "queue_token",
                tokenId, "Token " + token.getTokenNumber() + " cancelled");
        return token;
    }

    @Transactional
    public QueueToken recallToken(User staff, Long tokenId) {
        QueueToken token = getLockedToken(tokenId);
        assertStaffCanAccessQueue(staff, token.getQueueId());
        if (token.getStatus() != TokenStatus.SKIPPED) {
            throw AppException.badRequest("Only SKIPPED tokens can be recalled");
        }
        transition(token, staff.getId(), TokenStatus.CALLED, "Recalled by staff");
        notifyTokenCalled(token, staff);
        auditService.log(staff.getId(), "QUEUE_RECALL", "queue_token",
                tokenId, "Token " + token.getTokenNumber() + " recalled");
        return token;
    }

    // ──────────────────────────────────────────────
    // Queue status queries
    // ──────────────────────────────────────────────

    public List<QueueToken> getQueueTokens(Long queueId) {
        return tokenRepo.findByQueueIdOrderByCreatedAtAsc(queueId);
    }

    public List<QueueToken> getWaiting(Long queueId) {
        return tokenRepo.findByQueueIdAndStatusOrderByCreatedAtAsc(queueId, TokenStatus.WAITING);
    }

    public List<QueueToken> getActiveTokens(Long queueId) {
        return tokenRepo.findActiveTokens(queueId);
    }

    public QueueToken getQueueToken(Long queueId, Long tokenId) {
        return tokenRepo.findByIdAndQueueId(tokenId, queueId)
                .orElseThrow(() -> AppException.notFound("Queue token not found"));
    }

    public List<QueueToken> getPatientTokens(Long patientUserId) {
        return tokenRepo.findByPatientUserIdOrderByCreatedAtDesc(patientUserId);
    }

    public Optional<QueueToken> findTokenByAppointmentId(Long appointmentId) {
        return tokenRepo.findByAppointmentId(appointmentId);
    }

    // ──────────────────────────────────────────────
    // Waiting-time estimation
    // ──────────────────────────────────────────────

    public WaitEstimate estimateWait(Long queueId, Long tokenId) {
        QueueToken token = tokenRepo.findById(tokenId)
                .orElseThrow(() -> AppException.notFound("Token not found"));

        Doctor doctor = null;
        if (token.getDoctorId() != null) {
            doctor = doctorRepo.findById(token.getDoctorId()).orElse(null);
        }
        List<QueueToken> lane = tokenRepo.findByQueueIdOrderByCreatedAtAsc(queueId).stream()
                .filter(other -> token.getDoctorId() == null || token.getDoctorId().equals(other.getDoctorId()))
                .sorted(Comparator.comparing(QueueToken::getCreatedAt).thenComparing(QueueToken::getId))
                .toList();
        List<QueueToken> ahead = lane.stream()
                .filter(other -> !other.getId().equals(tokenId))
                .filter(other -> Comparator.comparing(QueueToken::getCreatedAt)
                        .thenComparing(QueueToken::getId).compare(other, token) < 0)
                .filter(other -> Set.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.IN_SERVICE)
                        .contains(other.getStatus()))
                .toList();
        QueueToken serving = lane.stream()
                .filter(other -> other.getStatus() == TokenStatus.IN_SERVICE)
                .findFirst().orElse(null);

        List<QueueToken> history = doctor == null ? List.of()
                : tokenRepo.findTop20ByDoctorIdAndStatusAndStartedAtIsNotNullAndCompletedAtIsNotNullOrderByCompletedAtDesc(
                        doctor.getId(), TokenStatus.COMPLETED);
        double averageMinutes = history.isEmpty()
                ? (doctor == null ? 15.0 : doctor.getConsultationMinutes())
                : history.stream()
                        .mapToLong(item -> Duration.between(item.getStartedAt(), item.getCompletedAt()).toSeconds())
                        .filter(seconds -> seconds > 0)
                        .average().orElse(doctor.getConsultationMinutes() * 60.0) / 60.0;
        averageMinutes = Math.max(1.0, averageMinutes);

        long patientsAhead = ahead.size();
        long waitingAhead = ahead.stream()
                .filter(other -> other.getStatus() == TokenStatus.WAITING || other.getStatus() == TokenStatus.CALLED)
                .count();
        long remainingCurrent = 0;
        if (serving != null && !serving.getId().equals(tokenId) && serving.getStartedAt() != null) {
            long elapsed = Math.max(0, Duration.between(serving.getStartedAt(), Instant.now()).toSeconds());
            remainingCurrent = Math.max(0, Math.round(averageMinutes * 60) - elapsed);
        }
        long totalSeconds = Math.max(0, remainingCurrent + Math.round(waitingAhead * averageMinutes * 60));
        int estimateMinutes = (int) Math.ceil(totalSeconds / 60.0);
        int lower = Math.max(0, estimateMinutes - 5);
        int upper = estimateMinutes == 0 ? 0 : estimateMinutes + 5;
        String confidence = history.size() >= 5 ? "HIGH" : history.isEmpty() ? "LOW" : "MEDIUM";
        String summary = patientsAhead == 0
                ? "You are next. The wait is approximate and may change."
                : "Approximately " + upper + " minutes remaining with " + patientsAhead +
                        (patientsAhead == 1 ? " patient" : " patients") + " ahead. This estimate may change.";
        String servingNumber = serving == null ? null : serving.getTokenNumber();
        Instant updatedAt = Instant.now();
        String arrival = null;
        if (upper > 0) {
            Queue queue = queueRepo.findById(queueId).orElseThrow(() -> AppException.notFound("Queue not found"));
            HospitalBranch branch = branchRepo.findById(queue.getBranchId())
                    .orElseThrow(() -> AppException.notFound("Hospital branch not found."));
            arrival = ZonedDateTime.ofInstant(updatedAt.plusSeconds(upper * 60L),
                    ZoneId.of(branch.getTimezone())).toOffsetDateTime().toString();
        }
        return new WaitEstimate(patientsAhead, lower, upper, confidence, summary,
                servingNumber, arrival, updatedAt);
    }

    private int estimateWaitMinutes(Long queueId, Long tokenId) {
        WaitEstimate e = estimateWait(queueId, tokenId);
        return e.upperMinutes();
    }

    // ──────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────

    private QueueToken getLockedToken(Long tokenId) {
        return tokenRepo.findByIdForUpdate(tokenId)
                .orElseThrow(() -> AppException.notFound("Token not found"));
    }

    private void transition(QueueToken token, Long actorId, TokenStatus next, String reason) {
        if (!token.getStatus().canTransitionTo(next)) {
            throw AppException.badRequest(
                    "Invalid transition: " + token.getStatus() + " → " + next);
        }
        TokenStatus prev = token.getStatus();
        Instant transitionTime = Instant.now();
        if (next == TokenStatus.IN_SERVICE) token.setStartedAt(transitionTime);
        if (next == TokenStatus.COMPLETED) token.setCompletedAt(transitionTime);
        token.setStatus(next);
        tokenRepo.save(token);
        recordEvent(token, actorId, prev, reason);
        eventPublisher.publishEvent(QueueUpdateEvent.from(token));
    }

    private void recordEvent(QueueToken token, Long actorId, TokenStatus prev, String reason) {
        QueueEvent event = new QueueEvent();
        event.setTokenId(token.getId());
        event.setActorUserId(actorId);
        event.setPreviousStatus(prev != null ? prev.name() : "NONE");
        event.setNextStatus(token.getStatus().name());
        event.setReason(reason);
        eventRepo.save(event);
    }

    private String generateTokenNumber(Queue queue) {
        int next = queue.getLastTokenSequence() + 1;
        queue.setLastTokenSequence(next);
        queueRepo.save(queue);
        return String.format("T%03d", next);
    }

    private void notifyTokenCalled(QueueToken token, User staff) {
        if (token.getPatientUserId() != null) {
            notificationService.createNotification(
                    token.getPatientUserId(),
                    "TOKEN_CALLED",
                    "It's Your Turn!",
                    "Token " + token.getTokenNumber() + " has been called. Please proceed to the consultation room.",
                    token.getTokenNumber());
        }
    }

    public record WaitEstimate(long patientsAhead, int lowerMinutes, int upperMinutes,
                               String confidence, String summary, String currentlyServingToken,
                               String estimatedArrival, Instant updatedAt) {}
}
