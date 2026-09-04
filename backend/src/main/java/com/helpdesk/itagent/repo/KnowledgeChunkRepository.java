package com.helpdesk.itagent.repo;

import com.helpdesk.itagent.domain.KnowledgeChunk;
import com.helpdesk.itagent.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findByDocument(KnowledgeDocument document);

    @Transactional
    void deleteByDocument(KnowledgeDocument document);
}
