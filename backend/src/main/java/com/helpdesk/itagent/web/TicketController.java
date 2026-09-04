package com.helpdesk.itagent.web;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Ticket;
import com.helpdesk.itagent.dto.TicketDtos;
import com.helpdesk.itagent.security.CurrentUserService;
import com.helpdesk.itagent.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserService currentUserService;

    public TicketController(TicketService ticketService, CurrentUserService currentUserService) {
        this.ticketService = ticketService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TicketDtos.TicketSummary> mine() {
        return ticketService.listSummaries(currentUserService.requireUser());
    }

    @GetMapping("/{id}")
    public TicketDtos.TicketDetail get(@PathVariable Long id) {
        return ticketService.getDetail(id, currentUserService.requireUser());
    }

    @PostMapping
    public TicketDtos.TicketDetail create(@RequestBody TicketDtos.CreateTicketRequest request) {
        AppUser user = currentUserService.requireUser();
        Ticket created = ticketService.createManual(user, request);
        return ticketService.getDetail(created.getId(), user);
    }

    @PostMapping("/{id}/comments")
    public TicketDtos.TicketDetail comment(@PathVariable Long id, @RequestBody TicketDtos.CommentRequest request) {
        AppUser user = currentUserService.requireUser();
        ticketService.addComment(id, request.body(), user);
        return ticketService.getDetail(id, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ticketService.delete(id, currentUserService.requireUser());
    }
}
