package com.helpdesk.itagent.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.itagent.domain.ChatMessage;
import com.helpdesk.itagent.domain.Conversation;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    void deleteByConversation(Conversation conversation);
    long countByRole(String role);
    long countByEscalatedTrue();
}
