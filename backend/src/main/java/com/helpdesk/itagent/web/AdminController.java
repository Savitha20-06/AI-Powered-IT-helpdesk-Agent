package com.helpdesk.itagent.web;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.helpdesk.itagent.dto.AdminDtos;
import com.helpdesk.itagent.dto.AuthDtos;
import com.helpdesk.itagent.security.CurrentUserService;
import com.helpdesk.itagent.service.AnalyticsService;
import com.helpdesk.itagent.service.KnowledgeBaseService;
import com.helpdesk.itagent.service.UserAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAdminService userAdminService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AdminController(UserAdminService userAdminService,
                           KnowledgeBaseService knowledgeBaseService,
                           AnalyticsService analyticsService,
                           CurrentUserService currentUserService) {
        this.userAdminService = userAdminService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users")
    public List<AdminDtos.UserView> users() {
        return userAdminService.list();
    }

    @PostMapping("/users")
    public AdminDtos.UserView createUser(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return userAdminService.create(request);
    }

    @PutMapping("/users/{id}")
    public AdminDtos.UserView updateUser(@PathVariable Long id, @RequestBody AdminDtos.UpdateUserRequest request) {
        return userAdminService.update(id, request, currentUserService.requireUser());
    }

    @GetMapping("/kb")
    public List<AdminDtos.DocumentView> documents() {
        return knowledgeBaseService.list();
    }

    @PostMapping("/kb")
    public AdminDtos.DocumentView upload(@RequestParam("file") MultipartFile file) throws IOException {
        return knowledgeBaseService.ingest(file, currentUserService.requireUser());
    }

    @PostMapping("/kb/defaults")
    public AdminDtos.DefaultLoadResult loadDefaults() throws IOException {
        return knowledgeBaseService.loadDefaultCompanyDocuments(currentUserService.requireUser());
    }

    @DeleteMapping("/kb/{id}")
    public void deleteDoc(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
    }

    @GetMapping("/analytics")
    public AdminDtos.AnalyticsResponse analytics() {
        return analyticsService.snapshot();
    }
}
