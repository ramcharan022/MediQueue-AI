package com.queueless.notification;

import com.queueless.security.UserDetailsServiceImpl;
import com.queueless.user.User;
import com.queueless.user.UserRepository;
import com.queueless.common.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    private final UserRepository userRepository;

    public NotificationController(NotificationService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    record NotificationResponse(Long id, String type, String title, String body,
                                String reference, boolean read, String createdAt) {}

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return service.getForUser(user.getId()).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(),
                        n.getBody(), n.getReference(), n.isRead(), n.getCreatedAt().toString()))
                .toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return Map.of("count", service.countUnread(user.getId()));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        service.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id) {
        User user = resolveUser(principal);
        service.markRead(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }
}
