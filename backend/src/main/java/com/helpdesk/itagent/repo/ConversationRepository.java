package com.helpdesk.itagent.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByOwnerOrderByUpdatedAtDesc(AppUser owner);
    Optional<Conversation> findByIdAndOwner(Long id, AppUser owner);
}
