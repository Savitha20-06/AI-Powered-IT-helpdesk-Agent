package com.helpdesk.itagent.dto;

import com.helpdesk.itagent.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, String email, String fullName, Role role, Long userId) {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String fullName,
            String department,
            Role role
    ) {}
}
