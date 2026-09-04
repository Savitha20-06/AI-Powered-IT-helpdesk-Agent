package com.helpdesk.itagent.dto;

import com.helpdesk.itagent.domain.Role;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AdminDtos {

    public record UserView(Long id, String email, String fullName, String department, Role role, boolean enabled, Instant createdAt) {}

    public record UpdateUserRequest(String fullName, String department, Role role, Boolean enabled, String password) {}

    public record DocumentView(Long id, String originalName, String contentType, long sizeBytes, int chunkCount, String status, Instant createdAt) {}

    public record DefaultLoadResult(int loaded, int skipped, List<String> loadedNames, List<String> skippedNames) {}

    public record AnalyticsResponse(
            long totalUsers,
            long totalTickets,
            long openTickets,
            long resolvedTickets,
            long aiEscalations,
            long kbDocuments,
            long kbChunks,
            long assistantMessages,
            Map<String, Long> ticketsByStatus,
            Map<String, Long> ticketsByPriority,
            Map<String, Long> ticketsByCategory
    ) {}
}
