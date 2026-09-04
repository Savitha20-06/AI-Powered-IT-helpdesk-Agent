package com.helpdesk.itagent.repo;

import com.helpdesk.itagent.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(com.helpdesk.itagent.domain.Role role);
}
