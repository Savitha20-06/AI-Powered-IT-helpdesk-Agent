package com.helpdesk.itagent.service;

import com.helpdesk.itagent.domain.Role;
import com.helpdesk.itagent.domain.TicketSource;
import com.helpdesk.itagent.domain.TicketStatus;
import com.helpdesk.itagent.dto.AdminDtos;
import com.helpdesk.itagent.repo.AppUserRepository;
import com.helpdesk.itagent.repo.ChatMessageRepository;
import com.helpdesk.itagent.repo.KnowledgeChunkRepository;
import com.helpdesk.itagent.repo.KnowledgeDocumentRepository;
import com.helpdesk.itagent.repo.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsService {

    private final AppUserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final ChatMessageRepository messageRepository;

    public AnalyticsService(AppUserRepository userRepository,
                            TicketRepository ticketRepository,
                            KnowledgeDocumentRepository documentRepository,
                            KnowledgeChunkRepository chunkRepository,
                            ChatMessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.messageRepository = messageRepository;
    }

    public AdminDtos.AnalyticsResponse snapshot() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        ticketRepository.countGroupByStatus().forEach(row -> byStatus.put(String.valueOf(row[0]), (Long) row[1]));
        Map<String, Long> byPriority = new LinkedHashMap<>();
        ticketRepository.countGroupByPriority().forEach(row -> byPriority.put(String.valueOf(row[0]), (Long) row[1]));
        Map<String, Long> byCategory = new LinkedHashMap<>();
        ticketRepository.countGroupByCategory().forEach(row -> byCategory.put(String.valueOf(row[0]), (Long) row[1]));

        return new AdminDtos.AnalyticsResponse(
                userRepository.count(),
                ticketRepository.count(),
                ticketRepository.countByStatus(TicketStatus.OPEN) + ticketRepository.countByStatus(TicketStatus.ASSIGNED)
                        + ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                ticketRepository.countByStatus(TicketStatus.RESOLVED) + ticketRepository.countByStatus(TicketStatus.CLOSED),
                ticketRepository.countBySource(TicketSource.AI_ESCALATION),
                documentRepository.count(),
                chunkRepository.count(),
                messageRepository.countByRole("assistant"),
                byStatus,
                byPriority,
                byCategory
        );
    }

    public long staffCount() {
        return userRepository.countByRole(Role.IT_STAFF) + userRepository.countByRole(Role.ADMIN);
    }
}
