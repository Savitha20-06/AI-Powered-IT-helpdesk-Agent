package com.helpdesk.itagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Role;
import com.helpdesk.itagent.repo.AppUserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KnowledgeBaseService knowledgeBaseService;

    public DataSeeder(AppUserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      KnowledgeBaseService knowledgeBaseService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public void run(String... args) throws Exception {
        AppUser admin = ensureUser("admin@helpdesk.local", "Password@123", "Asha Admin", "IT Governance", Role.ADMIN);
        ensureUser("itstaff@helpdesk.local", "Password@123", "Ravi Support", "IT Operations", Role.IT_STAFF);
        ensureUser("employee@helpdesk.local", "Password@123", "Meera Employee", "Finance", Role.EMPLOYEE);

        var result = knowledgeBaseService.loadDefaultCompanyDocuments(admin);
        log.info("Default company knowledge base: loaded {}, skipped {}", result.loaded(), result.skipped());
    }

    private AppUser ensureUser(String email, String password, String name, String dept, Role role) {
        AppUser user = userRepository.findByEmail(email).orElseGet(() -> {
            AppUser createdUser = new AppUser();
            createdUser.setEmail(email);
            createdUser.setPasswordHash(passwordEncoder.encode(password));
            createdUser.setFullName(name);
            createdUser.setDepartment(dept);
            createdUser.setRole(role);
            createdUser.setEnabled(true);
            return userRepository.save(createdUser);
        });
        boolean changed = false;
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(password));
            changed = true;
        }
        if (!user.isEnabled()) {
            user.setEnabled(true);
            changed = true;
        }
        if (user.getRole() != role) {
            user.setRole(role);
            changed = true;
        }
        if (!name.equals(user.getFullName())) {
            user.setFullName(name);
            changed = true;
        }
        if (!dept.equals(user.getDepartment())) {
            user.setDepartment(dept);
            changed = true;
        }
        if (changed) {
            user = userRepository.save(user);
        }
        return user;
    }
}
