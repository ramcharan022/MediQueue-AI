package com.queueless.auth.dto;

import com.queueless.user.UserRole;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserInfo user
) {
    public record UserInfo(Long id, String email, UserRole role, String displayName) {}

    public static AuthResponse of(String token, Long id, String email, UserRole role, String displayName) {
        return new AuthResponse(token, "Bearer", new UserInfo(id, email, role, displayName));
    }
}
