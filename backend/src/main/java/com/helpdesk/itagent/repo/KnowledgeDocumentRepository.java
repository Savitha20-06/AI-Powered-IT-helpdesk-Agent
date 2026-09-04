package com.helpdesk.itagent.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.itagent.domain.KnowledgeDocument;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    boolean existsByOriginalName(String originalName);
    Optional<KnowledgeDocument> findByOriginalName(String originalName);
}
