package com.helpdesk.itagent.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.helpdesk.itagent.config.AppProperties;
import com.helpdesk.itagent.domain.AppUser;
import com.helpdesk.itagent.domain.KnowledgeChunk;
import com.helpdesk.itagent.domain.KnowledgeDocument;
import com.helpdesk.itagent.dto.AdminDtos;
import com.helpdesk.itagent.rag.ChromaClient;
import com.helpdesk.itagent.rag.EmbeddingService;
import com.helpdesk.itagent.rag.TextChunker;
import com.helpdesk.itagent.rag.TextExtractor;
import com.helpdesk.itagent.repo.KnowledgeChunkRepository;
import com.helpdesk.itagent.repo.KnowledgeDocumentRepository;

@Service
@SuppressWarnings("null")
public class KnowledgeBaseService {

    private final AppProperties properties;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final TextExtractor textExtractor;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final ChromaClient chromaClient;

    public KnowledgeBaseService(AppProperties properties,
                                KnowledgeDocumentRepository documentRepository,
                                KnowledgeChunkRepository chunkRepository,
                                TextExtractor textExtractor,
                                TextChunker textChunker,
                                EmbeddingService embeddingService,
                                ChromaClient chromaClient) {
        this.properties = properties;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.chromaClient = chromaClient;
    }

    @Transactional
    public AdminDtos.DocumentView ingest(MultipartFile file, AppUser uploader) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        Path dir = Path.of(properties.getStorage().getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String storedName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setOriginalName(file.getOriginalFilename());
        document.setStoredPath(target.toString());
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setUploadedBy(uploader);
        document.setStatus("INDEXING");
        document = documentRepository.save(document);

        indexFile(document, target, file.getOriginalFilename());
        return toView(document);
    }

    @Transactional
    public KnowledgeDocument ingestText(String originalName, String text, AppUser uploader) throws IOException {
        Path dir = Path.of(properties.getStorage().getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "-" + originalName);
        Files.writeString(target, text);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setOriginalName(originalName);
        document.setStoredPath(target.toString());
        document.setContentType("text/plain");
        document.setSizeBytes(text.length());
        document.setUploadedBy(uploader);
        document.setStatus("INDEXING");
        document = documentRepository.save(document);
        indexFile(document, target, originalName);
        return document;
    }

    /**
     * Indexes the built-in Northwind IT runbooks from classpath:kb-samples.
     * Files that are already in the knowledge base (same file name) are skipped.
     */
    public AdminDtos.DefaultLoadResult loadDefaultCompanyDocuments(AppUser uploader) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:kb-samples/*");
        List<String> loaded = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        chromaClient.ensureCollection();
        for (Resource resource : resources) {
            String name = resource.getFilename();
            if (name == null || name.startsWith(".")) {
                continue;
            }
            var existing = documentRepository.findByOriginalName(name);
            if (existing.isPresent()) {
                reindex(existing.get());
                skipped.add(name);
                continue;
            }
            String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ingestText(name, text, uploader);
            loaded.add(name);
        }
        return new AdminDtos.DefaultLoadResult(loaded.size(), skipped.size(), loaded, skipped);
    }

    private void reindex(KnowledgeDocument document) {
        List<KnowledgeChunk> chunks = chunkRepository.findByDocument(document);
        if (chunks.isEmpty()) {
            return;
        }
        List<String> pieces = chunks.stream().map(KnowledgeChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.embedAll(pieces);
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> metas = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            ids.add("doc-" + document.getId() + "-chunk-" + chunk.getId());
            Map<String, Object> meta = new HashMap<>();
            meta.put("documentId", document.getId().intValue());
            meta.put("chunkId", chunk.getId().intValue());
            meta.put("sourceName", document.getOriginalName());
            metas.add(meta);
        }
        try {
            chromaClient.upsert(ids, embeddings, pieces, metas);
        } catch (Exception ignored) {
            // MySQL chunks remain available for local retrieval when Chroma is unavailable.
        }
    }

    private void indexFile(KnowledgeDocument document, Path file, String originalName) {
        String text = textExtractor.extract(file, originalName);
        List<String> pieces = textChunker.chunk(text);
        if (pieces.isEmpty()) {
            document.setStatus("EMPTY");
            document.setChunkCount(0);
            documentRepository.save(document);
            return;
        }
        List<KnowledgeChunk> savedChunks = new ArrayList<>();
        int index = 0;
        for (String piece : pieces) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(index++);
            chunk.setContent(piece);
            chunk.setSourceName(originalName);
            savedChunks.add(chunkRepository.save(chunk));
        }
        List<float[]> embeddings = embeddingService.embedAll(pieces);
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> metas = new ArrayList<>();
        for (KnowledgeChunk chunk : savedChunks) {
            ids.add("doc-" + document.getId() + "-chunk-" + chunk.getId());
            Map<String, Object> meta = new HashMap<>();
            meta.put("documentId", document.getId().intValue());
            meta.put("chunkId", chunk.getId().intValue());
            meta.put("sourceName", originalName);
            metas.add(meta);
        }
        try {
            chromaClient.upsert(ids, embeddings, pieces, metas);
        } catch (Exception ex) {
            // MySQL chunks remain so retrieval still works without Chroma.
        }
        document.setChunkCount(savedChunks.size());
        document.setStatus("READY");
        documentRepository.save(document);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        chromaClient.deleteByDocumentId(id);
        chunkRepository.deleteByDocument(document);
        documentRepository.delete(document);
        try {
            Files.deleteIfExists(Path.of(document.getStoredPath()));
        } catch (IOException ignored) {
        }
    }

    public List<AdminDtos.DocumentView> list() {
        return documentRepository.findAll().stream().map(this::toView).toList();
    }

    private AdminDtos.DocumentView toView(KnowledgeDocument document) {
        return new AdminDtos.DocumentView(
                document.getId(),
                document.getOriginalName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getChunkCount(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }
}
