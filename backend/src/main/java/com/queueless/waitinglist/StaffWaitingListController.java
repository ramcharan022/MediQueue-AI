package com.queueless.waitinglist;

import com.queueless.common.exception.AppException;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import com.queueless.queue.QueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/staff/departments/{departmentId}/waiting-list")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffWaitingListController {
    private final WaitingListService waitingListService;
    private final UserRepository userRepository;
    private final QueueService queueService;

    public StaffWaitingListController(WaitingListService waitingListService, UserRepository userRepository,
                                      QueueService queueService) {
        this.waitingListService = waitingListService;
        this.userRepository = userRepository;
        this.queueService = queueService;
    }

    record StaffEntryResponse(Long id, Long departmentId, Long doctorId, String status, Instant requestedAt) {
        static StaffEntryResponse from(WaitingListEntry entry) {
            return new StaffEntryResponse(entry.getId(), entry.getDepartmentId(), entry.getDoctorId(),
                    entry.getStatus().name(), entry.getRequestedAt());
        }
    }

    @GetMapping
    public List<StaffEntryResponse> list(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable Long departmentId) {
        queueService.assertStaffCanAccessDepartment(resolveUser(principal), departmentId);
        return waitingListService.listPendingForStaff(departmentId).stream()
                .map(StaffEntryResponse::from).toList();
    }

    @PostMapping("/{entryId}/offer-opening")
    public StaffEntryResponse offerOpening(@AuthenticationPrincipal UserDetails principal,
                                           @PathVariable Long departmentId, @PathVariable Long entryId) {
        User staff = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found."));
        queueService.assertStaffCanAccessDepartment(staff, departmentId);
        return StaffEntryResponse.from(waitingListService.offerOpening(staff, departmentId, entryId));
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found."));
    }
}
