package com.helpdesk.itagent.web;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.dto.AdminDtos;
import com.helpdesk.itagent.dto.TicketDtos;
import com.helpdesk.itagent.security.CurrentUserService;
import com.helpdesk.itagent.service.TicketService;
import com.helpdesk.itagent.service.UserAdminService;

@RestController
@RequestMapping("/api/staff/tickets")
public class StaffTicketController {

    private final TicketService ticketService;
    private final CurrentUserService currentUserService;
    private final UserAdminService userAdminService;

    public StaffTicketController(TicketService ticketService,
                                 CurrentUserService currentUserService,
                                 UserAdminService userAdminService) {
        this.ticketService = ticketService;
        this.currentUserService = currentUserService;
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<TicketDtos.TicketSummary> all() {
        return ticketService.listSummaries(currentUserService.requireUser());
    }

    @PatchMapping("/{id}/assign")
    public TicketDtos.TicketDetail assign(@PathVariable Long id, @RequestBody TicketDtos.AssignRequest request) {
        AppUser user = currentUserService.requireUser();
        ticketService.assign(id, request.assigneeId(), user);
        return ticketService.getDetail(id, user);
    }

    @PatchMapping("/{id}/status")
    public TicketDtos.TicketDetail status(@PathVariable Long id, @RequestBody TicketDtos.StatusRequest request) {
        AppUser user = currentUserService.requireUser();
        ticketService.updateStatus(id, request.status(), user);
        return ticketService.getDetail(id, user);
    }

    @PatchMapping("/{id}")
    public TicketDtos.TicketDetail update(@PathVariable Long id, @RequestBody TicketDtos.UpdateTicketRequest request) {
        AppUser user = currentUserService.requireUser();
        ticketService.update(id, request, user);
        return ticketService.getDetail(id, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ticketService.delete(id, currentUserService.requireUser());
    }

    @GetMapping("/assignees")
    public List<AdminDtos.UserView> assignees() {
        return userAdminService.staff();
    }
}
