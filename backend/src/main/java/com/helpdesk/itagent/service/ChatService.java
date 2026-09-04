package com.helpdesk.itagent.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.ChatMessage;
import com.helpdesk.itagent.domain.Conversation;
import com.helpdesk.itagent.domain.Ticket;
import com.helpdesk.itagent.dto.ChatDtos;
import com.helpdesk.itagent.rag.AiDecision;
import com.helpdesk.itagent.rag.DomainRelevanceService;
import com.helpdesk.itagent.rag.LlmService;
import com.helpdesk.itagent.rag.RagService;
import com.helpdesk.itagent.rag.RetrievedChunk;
import com.helpdesk.itagent.repo.ChatMessageRepository;
import com.helpdesk.itagent.repo.ConversationRepository;
import com.helpdesk.itagent.repo.TicketRepository;

@Service
@SuppressWarnings("null")
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final RagService ragService;
    private final LlmService llmService;
    private final TicketService ticketService;
    private final DomainRelevanceService domainRelevanceService;

    public ChatService(ConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       TicketRepository ticketRepository,
                       RagService ragService,
                       LlmService llmService,
                       TicketService ticketService,
                       DomainRelevanceService domainRelevanceService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ticketRepository = ticketRepository;
        this.ragService = ragService;
        this.llmService = llmService;
        this.ticketService = ticketService;
        this.domainRelevanceService = domainRelevanceService;
    }

    public List<ChatDtos.ConversationSummary> listConversations(AppUser user) {
        return conversationRepository.findByOwnerOrderByUpdatedAtDesc(user).stream()
                .map(c -> new ChatDtos.ConversationSummary(c.getId(), c.getTitle(), c.getUpdatedAt()))
                .toList();
    }

    public Conversation getOwned(Long id, AppUser user) {
        return conversationRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    public List<ChatDtos.MessageView> messages(Long conversationId, AppUser user) {
        Conversation conversation = getOwned(conversationId, user);
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public void deleteConversation(Long conversationId, AppUser user) {
        Conversation conversation = getOwned(conversationId, user);
        deleteConversationData(conversation);
    }

    @Transactional
    public void deleteAllConversations(AppUser user) {
        conversationRepository.findByOwnerOrderByUpdatedAtDesc(user)
                .forEach(this::deleteConversationData);
    }

    private void deleteConversationData(Conversation conversation) {
        ticketRepository.findByConversation(conversation).forEach(ticket -> ticket.setConversation(null));
        ticketRepository.flush();
        messageRepository.deleteByConversation(conversation);
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ChatDtos.AskResponse ask(AppUser user, ChatDtos.AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }
        Conversation conversation = resolveConversation(user, request.conversationId(), request.question());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setRole("user");
        userMessage.setContent(request.question().trim());
        userMessage = messageRepository.save(userMessage);

        if (!domainRelevanceService.isCompanyItQuestion(request.question())) {
            ChatMessage assistant = new ChatMessage();
            assistant.setConversation(conversation);
            assistant.setRole("assistant");
            assistant.setContent("I'm sorry, I can only assist with company-related IT support queries. Would you like to create a support ticket?");
            assistant.setEscalated(false);
            assistant.setUsedRag(false);
            assistant = messageRepository.save(assistant);
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
            return new ChatDtos.AskResponse(
                new ChatDtos.ConversationSummary(conversation.getId(), conversation.getTitle(), conversation.getUpdatedAt()),
                toView(userMessage),
                toView(assistant),
                null,
                List.of(),
                false,
                true
            );
        }

        List<RetrievedChunk> chunks = ragService.retrieve(request.question());
        boolean sufficient = ragService.sufficient(request.question(), chunks);
        AiDecision decision = llmService.answer(request.question(), chunks, sufficient);
        boolean escalate = !decision.sufficientKnowledge();

        ChatMessage assistant = new ChatMessage();
        assistant.setConversation(conversation);
        assistant.setRole("assistant");
        assistant.setContent(decision.answer());
        assistant.setCategory(decision.category());
        assistant.setPriority(decision.priority());
        assistant.setConfidence(decision.confidence());
        assistant.setUsedRag(!chunks.isEmpty());
        assistant.setEscalated(escalate);
        assistant = messageRepository.save(assistant);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        Ticket created = null;
        if (escalate) {
            String kb = chunks.stream()
                    .map(c -> "[" + c.sourceName() + " | score=" + String.format("%.2f", c.similarity()) + "]\n" + c.content())
                    .collect(Collectors.joining("\n\n"));
            created = ticketService.createFromAi(
                    user,
                    conversation,
                    "AI Escalation: " + decision.summary(),
                    decision.summary(),
                    decision.symptoms(),
                    decision.troubleshootingAttempted(),
                    kb.isBlank() ? "No sufficiently similar knowledge-base chunk was retrieved." : kb,
                    decision.category(),
                    decision.priority()
            );
            assistant.setContent(decision.answer() + "\n\nTicket " + created.getTicketNumber()
                    + " was created with suggested priority " + created.getPriority()
                    + " and category " + created.getCategory() + ".");
            assistant = messageRepository.save(assistant);
        }

        return new ChatDtos.AskResponse(
                new ChatDtos.ConversationSummary(conversation.getId(), conversation.getTitle(), conversation.getUpdatedAt()),
                toView(userMessage),
                toView(assistant),
                created == null ? null : ticketService.toSummary(created),
                chunks.stream().map(RetrievedChunk::sourceName).distinct().toList(),
                decision.sufficientKnowledge(),
                false
        );
    }

    private Conversation resolveConversation(AppUser user, Long conversationId, String question) {
        if (conversationId != null) {
            return getOwned(conversationId, user);
        }
        Conversation conversation = new Conversation();
        conversation.setOwner(user);
        String title = question.trim();
        conversation.setTitle(title.length() > 72 ? title.substring(0, 72) + "..." : title);
        return conversationRepository.save(conversation);
    }

    private ChatDtos.MessageView toView(ChatMessage message) {
        return new ChatDtos.MessageView(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCategory(),
                message.getPriority(),
                message.getConfidence(),
                message.isUsedRag(),
                message.isEscalated(),
                message.getCreatedAt()
        );
    }
}
