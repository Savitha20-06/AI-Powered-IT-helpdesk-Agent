package com.helpdesk.itagent.dto;

import com.helpdesk.itagent.domain.IssueCategory;
import com.helpdesk.itagent.domain.TicketPriority;
import com.helpdesk.itagent.domain.TicketSource;
import com.helpdesk.itagent.domain.TicketStatus;

import java.time.Instant;
import java.util.List;

public class TicketDtos {

    public record UserRef(Long id, String fullName, String email) {}

    public record CommentView(Long id, UserRef author, String body, Instant createdAt) {}

    public record TicketSummary(
            Long id,
            String ticketNumber,
            String title,
            IssueCategory category,
            TicketPriority priority,
            TicketStatus status,
            TicketSource source,
            UserRef employee,
            UserRef assignedTo,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TicketDetail(
            Long id,
            String ticketNumber,
            String title,
            String summary,
            String symptoms,
            String troubleshootingAttempted,
            String kbContext,
            IssueCategory category,
            TicketPriority priority,
            TicketStatus status,
            TicketSource source,
            UserRef employee,
            UserRef assignedTo,
            Long conversationId,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt,
            Instant closedAt,
            List<CommentView> comments
    ) {}

    public record CreateTicketRequest(
            String title,
            String summary,
            String symptoms,
            IssueCategory category,
            TicketPriority priority
    ) {}

    public record AssignRequest(Long assigneeId) {}

    public record StatusRequest(TicketStatus status) {}

    public record CommentRequest(String body) {}

    public record UpdateTicketRequest(
            TicketPriority priority,
            IssueCategory category,
            String title
    ) {}
}
