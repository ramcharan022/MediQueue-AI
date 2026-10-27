package com.queueless.appointment;

import com.queueless.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.queueless.user.UserRepository;
import com.queueless.common.exception.AppException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Appointment> book(@Valid @RequestBody BookingRequest req,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.book(resolveUser(userDetails), req));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<Appointment> myAppointments(@AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.getMyAppointments(resolveUser(userDetails));
    }

    @GetMapping("/{reference}")
    @PreAuthorize("hasRole('PATIENT')")
    public Appointment getOne(@PathVariable String reference,
                               @AuthenticationPrincipal UserDetails userDetails) {
        return appointmentService.getByReference(resolveUser(userDetails), reference);
    }

    @PatchMapping("/{reference}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable String reference,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        Appointment appt = appointmentService.cancel(resolveUser(userDetails), reference);
        return ResponseEntity.ok(Map.of("reference", appt.getReference(), "status", appt.getStatus().name()));
    }
}
