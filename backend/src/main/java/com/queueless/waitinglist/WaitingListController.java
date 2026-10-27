package com.queueless.waitinglist;

import com.queueless.common.exception.AppException;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/waiting-list")
@PreAuthorize("hasRole('PATIENT')")
public class WaitingListController {
    private final WaitingListService waitingListService;
    private final UserRepository userRepository;

    public WaitingListController(WaitingListService waitingListService, UserRepository userRepository) {
        this.waitingListService = waitingListService;
        this.userRepository = userRepository;
    }

    record JoinRequest(@NotNull Long departmentId, Long doctorId) {}
    record EntryResponse(Long id, Long departmentId, Long doctorId, String status, Instant requestedAt) {
        static EntryResponse from(WaitingListEntry entry) {
            return new EntryResponse(entry.getId(), entry.getDepartmentId(), entry.getDoctorId(),
                    entry.getStatus().name(), entry.getRequestedAt());
        }
    }

    @GetMapping
    public List<EntryResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return waitingListService.list(resolveUser(principal)).stream().map(EntryResponse::from).toList();
    }

    @PostMapping
    public EntryResponse join(@AuthenticationPrincipal UserDetails principal,
                              @Valid @RequestBody JoinRequest request) {
        return EntryResponse.from(waitingListService.join(resolveUser(principal),
                request.departmentId(), request.doctorId()));
    }

    @DeleteMapping("/{id}")
    public EntryResponse cancel(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return EntryResponse.from(waitingListService.cancel(resolveUser(principal), id));
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found."));
    }
}
