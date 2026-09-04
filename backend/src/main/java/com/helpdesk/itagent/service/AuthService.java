package com.helpdesk.itagent.service;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Role;
import com.helpdesk.itagent.dto.AdminDtos;
import com.helpdesk.itagent.dto.AuthDtos;
import com.helpdesk.itagent.repo.AppUserRepository;
import com.helpdesk.itagent.security.JwtService;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!user.isEnabled()
            || user.getPasswordHash() == null
            || user.getRole() == null
            || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toAuth(user);
    }

    public AppUser createUser(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setDepartment(request.department());
        user.setRole(request.role() == null ? Role.EMPLOYEE : request.role());
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public AuthDtos.AuthResponse toAuth(AppUser user) {
        String token = jwtService.generateToken(user.getEmail(), Map.of("role", user.getRole().name()));
        return new AuthDtos.AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole(), user.getId());
    }

    public AdminDtos.UserView toView(AppUser user) {
        return new AdminDtos.UserView(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getDepartment(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
