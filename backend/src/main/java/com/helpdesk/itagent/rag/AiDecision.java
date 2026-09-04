package com.helpdesk.itagent.rag;

import com.helpdesk.itagent.domain.IssueCategory;
import com.helpdesk.itagent.domain.TicketPriority;

public record AiDecision(
        String answer,
        boolean sufficientKnowledge,
        IssueCategory category,
        TicketPriority priority,
        double confidence,
        String summary,
        String symptoms,
        String troubleshootingAttempted
) {}
