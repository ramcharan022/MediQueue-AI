package com.queueless.user;

import com.queueless.common.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final UserRepository userRepo;
    private final PatientProfileRepository profileRepo;

    public ProfileController(UserRepository userRepo, PatientProfileRepository profileRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
    }

    record ProfileResponse(Long userId, String email, String role, String displayName,
                            String phone, String dateOfBirth) {}

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        PatientProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);
        return ResponseEntity.ok(new ProfileResponse(
                user.getId(), user.getEmail(), user.getRole().name(),
                profile != null ? profile.getDisplayName() : user.getEmail(),
                profile != null ? profile.getPhone() : null,
                profile != null && profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : null
        ));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal UserDetails principal,
                                                          @RequestBody Map<String, String> body) {
        User user = resolveUser(principal);
        PatientProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);

        if (profile != null) {
            if (body.containsKey("displayName")) profile.setDisplayName(body.get("displayName"));
            if (body.containsKey("phone")) profile.setPhone(body.get("phone"));
            profileRepo.save(profile);
        }

        return getProfile(principal);
    }

    private User resolveUser(UserDetails principal) {
        return userRepo.findByEmail(principal.getUsername())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }
}
