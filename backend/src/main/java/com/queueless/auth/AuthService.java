package com.queueless.auth;

import com.queueless.auth.dto.AuthResponse;
import com.queueless.auth.dto.AdminBootstrapRequest;
import com.queueless.auth.dto.LoginRequest;
import com.queueless.auth.dto.RegisterRequest;
import com.queueless.common.exception.AppException;
import com.queueless.security.JwtService;
import com.queueless.security.UserDetailsServiceImpl;
import com.queueless.user.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final String adminBootstrapToken;

    public AuthService(UserRepository userRepository,
                       PatientProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserDetailsServiceImpl userDetailsService,
                       AuthenticationManager authenticationManager,
                       @org.springframework.beans.factory.annotation.Value("${app.admin.bootstrap-token:}") String adminBootstrapToken) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
        this.adminBootstrapToken = adminBootstrapToken;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw AppException.conflict("Email address is already registered.");
        }

        User user = new User(
                req.email().toLowerCase().trim(),
                passwordEncoder.encode(req.password()),
                UserRole.PATIENT
        );
        userRepository.save(user);

        PatientProfile profile = new PatientProfile(user.getId(), req.displayName().trim());
        profileRepository.save(profile);

        String token = generateToken(user);
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getRole(), profile.getDisplayName());
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email().toLowerCase().trim(), req.password())
            );
        } catch (AuthenticationException e) {
            throw AppException.badRequest("Invalid email or password.");
        }

        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> AppException.badRequest("Invalid email or password."));

        String displayName = resolveDisplayName(user);
        String token = generateToken(user);
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getRole(), displayName);
    }

    @Transactional
    public AuthResponse bootstrapAdmin(AdminBootstrapRequest request) {
        if (adminBootstrapToken.isBlank() || adminBootstrapToken.length() < 32
                || adminBootstrapToken.startsWith("replace-with-")) {
            throw AppException.forbidden("Admin bootstrap is not enabled. Configure ADMIN_BOOTSTRAP_TOKEN locally first.");
        }
        boolean validToken = MessageDigest.isEqual(
                adminBootstrapToken.getBytes(StandardCharsets.UTF_8),
                request.bootstrapToken().getBytes(StandardCharsets.UTF_8));
        if (!validToken) {
            throw AppException.forbidden("Invalid admin bootstrap token.");
        }

        userRepository.lockAdminBootstrap();
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw AppException.conflict("An administrator account has already been created.");
        }
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw AppException.conflict("Email address is already registered.");
        }

        User admin = userRepository.save(new User(email,
                passwordEncoder.encode(request.password()), UserRole.ADMIN));
        return AuthResponse.of(generateToken(admin), admin.getId(), admin.getEmail(),
                admin.getRole(), admin.getEmail());
    }

    private String generateToken(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));
    }

    private String resolveDisplayName(User user) {
        if (user.getRole() == UserRole.PATIENT) {
            return profileRepository.findByUserId(user.getId())
                    .map(PatientProfile::getDisplayName)
                    .orElse(user.getEmail());
        }
        return user.getEmail();
    }
}
