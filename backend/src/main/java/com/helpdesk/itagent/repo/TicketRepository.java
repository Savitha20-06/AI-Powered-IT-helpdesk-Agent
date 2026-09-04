package com.helpdesk.itagent.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Conversation;
import com.helpdesk.itagent.domain.IssueCategory;
import com.helpdesk.itagent.domain.Ticket;
import com.helpdesk.itagent.domain.TicketPriority;
import com.helpdesk.itagent.domain.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @EntityGraph(attributePaths = {"employee", "assignedTo"})
    List<Ticket> findByEmployeeOrderByCreatedAtDesc(AppUser employee);

    @EntityGraph(attributePaths = {"employee", "assignedTo"})
    List<Ticket> findByAssignedToOrderByCreatedAtDesc(AppUser assignedTo);

    @EntityGraph(attributePaths = {"employee", "assignedTo"})
    List<Ticket> findAllByOrderByCreatedAtDesc();

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByConversation(Conversation conversation);

    @EntityGraph(attributePaths = {"employee", "assignedTo", "conversation"})
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findWithDetailsById(@Param("id") Long id);
    long countByStatus(TicketStatus status);
    long countByPriority(TicketPriority priority);
    long countByCategory(IssueCategory category);
    long countBySource(com.helpdesk.itagent.domain.TicketSource source);

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countGroupByStatus();

    @Query("select t.priority, count(t) from Ticket t group by t.priority")
    List<Object[]> countGroupByPriority();

    @Query("select t.category, count(t) from Ticket t group by t.category")
    List<Object[]> countGroupByCategory();
}
