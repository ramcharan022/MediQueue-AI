package com.queueless.queue;

import com.queueless.common.exception.AppException;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import com.queueless.user.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class QueueController {

    private final QueueService queueService;
    private final UserRepository userRepo;

    public QueueController(QueueService queueService, UserRepository userRepo) {
        this.queueService = queueService;
        this.userRepo = userRepo;
    }

    record JoinRequest(@NotNull Long branchId, @NotNull Long departmentId, Long doctorId, Long appointmentId) {}

    record TokenResponse(Long id, String tokenNumber, String status,
                         Long queueId, Long doctorId, Long appointmentId,
                         Integer estimatedWaitMinutes, String allocationReason,
                         String createdAt, String updatedAt) {}

    record QueueStatusResponse(Long queueId, long waitingCount, long calledCount,
                                List<TokenResponse> tokens) {}

    record WaitEstimateResponse(long patientsAhead, int lowerMinutes, int upperMinutes,
                                 String confidence, String summary, String currentlyServingToken,
                                 String estimatedArrival, String updatedAt) {}
    record CheckInAppointmentResponse(Long id, String reference, Long doctorId,
                                      LocalDate serviceDate, String startsAt, String status) {}
    record TokenReasonRequest(@Size(max = 500) String reason) {}

    // ── Patient: join queue
    @PostMapping("/queues/join")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<TokenResponse> join(@AuthenticationPrincipal UserDetails principal,
                                               @Valid @RequestBody JoinRequest req) {
        User user = resolveUser(principal);
        QueueToken token = queueService.joinQueue(user, req.branchId(), req.departmentId(),
                req.doctorId(), req.appointmentId());
        return ResponseEntity.status(201).body(toTokenResponse(token));
    }

    // ── Staff: full queue status
    @GetMapping("/queues/{queueId}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<QueueStatusResponse> status(@AuthenticationPrincipal UserDetails principal,
                                                        @PathVariable Long queueId) {
        queueService.assertStaffCanAccessQueue(resolveUser(principal), queueId);
        queueService.getQueue(queueId); // validates existence
        List<QueueToken> tokens = queueService.getQueueTokens(queueId);
        long waiting = tokens.stream().filter(t -> t.getStatus() == TokenStatus.WAITING).count();
        long called = tokens.stream().filter(t -> t.getStatus() == TokenStatus.CALLED || t.getStatus() == TokenStatus.IN_SERVICE).count();
        List<TokenResponse> tokenResponses = tokens.stream().map(this::toTokenResponse).toList();
        return ResponseEntity.ok(new QueueStatusResponse(queueId, waiting, called, tokenResponses));
    }

    @GetMapping("/queues/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<TokenResponse> myTokens(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return queueService.getPatientTokens(user.getId()).stream().map(this::toTokenResponse).toList();
    }

    // ── Patient: estimate wait for my token
    @GetMapping("/queues/{queueId}/tokens/{tokenId}/estimate")
    public ResponseEntity<WaitEstimateResponse> estimate(@PathVariable Long queueId,
                                                          @PathVariable Long tokenId,
                                                          @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        QueueToken token = queueService.getQueueToken(queueId, tokenId);
        if (user.getRole() == UserRole.PATIENT) {
            if (!user.getId().equals(token.getPatientUserId())) {
                throw AppException.forbidden("You can only view your own queue estimate.");
            }
        } else {
            queueService.assertStaffCanAccessQueue(user, queueId);
        }
        QueueService.WaitEstimate est = queueService.estimateWait(queueId, tokenId);
        return ResponseEntity.ok(new WaitEstimateResponse(est.patientsAhead(), est.lowerMinutes(),
                est.upperMinutes(), est.confidence(), est.summary(), est.currentlyServingToken(),
                est.estimatedArrival(), est.updatedAt().toString()));
    }

    // ── Staff: call next
    @PostMapping("/staff/queues/{queueId}/call-next")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> callNext(@AuthenticationPrincipal UserDetails principal,
                                                   @PathVariable Long queueId) {
        User staff = resolveUser(principal);
        QueueToken token = queueService.callNext(staff, queueId);
        return ResponseEntity.ok(toTokenResponse(token));
    }

    // ── Staff: start consultation
    @PostMapping("/staff/tokens/{tokenId}/start")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> startConsultation(@AuthenticationPrincipal UserDetails principal,
                                                            @PathVariable Long tokenId) {
        User staff = resolveUser(principal);
        return ResponseEntity.ok(toTokenResponse(queueService.startConsultation(staff, tokenId)));
    }

    // ── Staff: complete consultation
    @PostMapping("/staff/tokens/{tokenId}/complete")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> complete(@AuthenticationPrincipal UserDetails principal,
                                                   @PathVariable Long tokenId) {
        User staff = resolveUser(principal);
        return ResponseEntity.ok(toTokenResponse(queueService.completeConsultation(staff, tokenId)));
    }

    // ── Staff: skip token
    @PostMapping("/staff/tokens/{tokenId}/skip")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> skip(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long tokenId,
                                               @Valid @RequestBody(required = false) TokenReasonRequest body) {
        User staff = resolveUser(principal);
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(toTokenResponse(queueService.skipToken(staff, tokenId, reason)));
    }

    // ── Staff: no-show
    @PostMapping("/staff/tokens/{tokenId}/no-show")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> noShow(@AuthenticationPrincipal UserDetails principal,
                                                 @PathVariable Long tokenId) {
        User staff = resolveUser(principal);
        return ResponseEntity.ok(toTokenResponse(queueService.markNoShow(staff, tokenId)));
    }

    @PostMapping("/staff/tokens/{tokenId}/cancel")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> cancelToken(@AuthenticationPrincipal UserDetails principal,
                                                      @PathVariable Long tokenId,
                                                      @Valid @RequestBody(required = false) TokenReasonRequest body) {
        User staff = resolveUser(principal);
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(toTokenResponse(queueService.cancelToken(staff, tokenId, reason)));
    }

    // ── Staff: recall skipped
    @PostMapping("/staff/tokens/{tokenId}/recall")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> recall(@AuthenticationPrincipal UserDetails principal,
                                                 @PathVariable Long tokenId) {
        User staff = resolveUser(principal);
        return ResponseEntity.ok(toTokenResponse(queueService.recallToken(staff, tokenId)));
    }

    // ── Staff: get waiting list for queue
    @GetMapping("/staff/queues/{queueId}/waiting")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<TokenResponse> waitingList(@AuthenticationPrincipal UserDetails principal,
                                           @PathVariable Long queueId) {
        queueService.assertStaffCanAccessQueue(resolveUser(principal), queueId);
        return queueService.getWaiting(queueId).stream().map(this::toTokenResponse).toList();
    }

    @GetMapping("/staff/appointments")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<CheckInAppointmentResponse> checkInAppointments(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam Long branchId,
            @RequestParam Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queueService.getCheckInAppointments(resolveUser(principal), branchId, departmentId, date)
                .stream()
                .map(appointment -> new CheckInAppointmentResponse(appointment.getId(), appointment.getReference(),
                        appointment.getDoctorId(), appointment.getStartsAt().toLocalDate(),
                        appointment.getStartsAt().toString(), appointment.getStatus().name()))
                .toList();
    }

    @PostMapping("/staff/appointments/{appointmentId}/check-in")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TokenResponse> checkIn(
            @AuthenticationPrincipal UserDetails principal, @PathVariable Long appointmentId) {
        return ResponseEntity.status(201).body(toTokenResponse(
                queueService.checkInAppointment(resolveUser(principal), appointmentId)));
    }

    // ── Resolve or create queue by branch+department
    @PostMapping("/queues/resolve")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<Map<String, Long>> resolveQueue(@AuthenticationPrincipal UserDetails principal,
                                                           @RequestBody Map<String, Long> req) {
        Long branchId = req.get("branchId");
        Long departmentId = req.get("departmentId");
        if (branchId == null || departmentId == null) throw AppException.badRequest("branchId and departmentId required");
        queueService.assertStaffCanAccessQueueScope(resolveUser(principal), branchId, departmentId);
        Queue q = queueService.getOrCreateQueue(branchId, departmentId);
        return ResponseEntity.ok(Map.of("queueId", q.getId()));
    }

    private User resolveUser(UserDetails principal) {
        return userRepo.findByEmail(principal.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    private TokenResponse toTokenResponse(QueueToken t) {
        return new TokenResponse(t.getId(), t.getTokenNumber(), t.getStatus().name(),
                t.getQueueId(), t.getDoctorId(), t.getAppointmentId(),
                t.getEstimatedWaitMinutes(), t.getAllocationReason(),
                t.getCreatedAt().toString(), t.getUpdatedAt().toString());
    }
}
