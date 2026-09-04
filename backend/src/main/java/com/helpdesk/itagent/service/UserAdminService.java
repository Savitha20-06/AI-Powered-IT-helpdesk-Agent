package com.helpdesk.itagent.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Role;
import com.helpdesk.itagent.dto.AdminDtos;
import com.helpdesk.itagent.dto.AuthDtos;
import com.helpdesk.itagent.repo.AppUserRepository;

@Service
@SuppressWarnings("null")
public class UserAdminService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserAdminService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public List<AdminDtos.UserView> list() {
        return userRepository.findAll().stream().map(authService::toView).toList();
    }

    public AdminDtos.UserView create(AuthDtos.RegisterRequest request) {
        return authService.toView(authService.createUser(request));
    }

    public AdminDtos.UserView update(Long id, AdminDtos.UpdateUserRequest request, AppUser actor) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (actor.getId().equals(id) && Boolean.FALSE.equals(request.enabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own admin account");
        }
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.department() != null) user.setDepartment(request.department());
        if (request.role() != null) user.setRole(request.role());
        if (request.enabled() != null) user.setEnabled(request.enabled());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return authService.toView(userRepository.save(user));
    }

    public List<AdminDtos.UserView> staff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.IT_STAFF || u.getRole() == Role.ADMIN)
                .map(authService::toView)
                .toList();
    }
}
