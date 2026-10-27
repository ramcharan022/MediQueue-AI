package com.queueless.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBootstrapRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank String bootstrapToken
) {}
