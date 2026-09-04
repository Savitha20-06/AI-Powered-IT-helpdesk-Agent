package com.helpdesk.itagent.dto;

import java.time.Instant;
import java.util.List;

import com.helpdesk.itagent.domain.IssueCategory;
import com.helpdesk.itagent.domain.TicketPriority;

public class ChatDtos {

    public record ConversationSummary(Long id, String title, Instant updatedAt) {}

    public record MessageView(
            Long id,
            String role,
            String content,
            IssueCategory category,
            TicketPriority priority,
            Double confidence,
            boolean usedRag,
            boolean escalated,
            Instant createdAt
    ) {}

    public record AskRequest(Long conversationId, String question) {}

    public record AskResponse(
            ConversationSummary conversation,
            MessageView userMessage,
            MessageView assistantMessage,
            TicketDtos.TicketSummary createdTicket,
            List<String> sources,
                        boolean sufficientKnowledge,
                        boolean ticketRecommended
    ) {}
}
