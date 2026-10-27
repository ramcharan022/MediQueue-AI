package com.queueless.auth;

import com.queueless.auth.dto.AuthResponse;
import com.queueless.auth.dto.AdminBootstrapRequest;
import com.queueless.auth.dto.LoginRequest;
import com.queueless.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<AuthResponse> bootstrapAdmin(@Valid @RequestBody AdminBootstrapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.bootstrapAdmin(request));
    }
}
