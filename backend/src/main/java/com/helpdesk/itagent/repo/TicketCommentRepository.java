package com.helpdesk.itagent.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.itagent.domain.Ticket;
import com.helpdesk.itagent.domain.TicketComment;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    void deleteByTicket(Ticket ticket);
}
