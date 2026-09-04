package com.helpdesk.itagent.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.helpdesk.itagent.config.AppProperties;
import com.helpdesk.itagent.domain.KnowledgeChunk;
import com.helpdesk.itagent.repo.KnowledgeChunkRepository;

@Service
@SuppressWarnings("null")
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final Map<String, List<String>> INTENT_KEYWORDS = Map.of(
            "vpn", List.of("vpn", "anyconnect", "secure client", "remote access", "gateway", "tunnel", "remote", "corporate network"),
            "email", List.of("email", "outlook", "mailbox", "exchange", "send", "receive", "owa"),
            "network", List.of("wifi", "wi-fi", "network", "internet", "intranet", "ethernet", "dns", "connectivity"),
            "printer", List.of("printer", "print", "toner", "paper jam", "queue"),
            "software", List.of("software", "install", "software center", "office", "teams", "license", "application"),
            "password", List.of("password", "mfa", "reset", "login", "account", "locked out", "authentication"),
            "hardware", List.of("laptop", "monitor", "keyboard", "mouse", "battery", "hardware", "device"),
            "access", List.of("permission", "request", "approval", "sharepoint", "role", "authorize")
    );

    private final AppProperties properties;
    private final EmbeddingService embeddingService;
    private final ChromaClient chromaClient;
    private final KnowledgeChunkRepository chunkRepository;

    public RagService(AppProperties properties,
                      EmbeddingService embeddingService,
                      ChromaClient chromaClient,
                      KnowledgeChunkRepository chunkRepository) {
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.chromaClient = chromaClient;
        this.chunkRepository = chunkRepository;
    }

    public List<RetrievedChunk> retrieve(String question) {
        float[] queryVector = embeddingService.embed(question);
        int topK = properties.getRag().getTopK();
        List<RetrievedChunk> chromaHits = chromaClient.query(queryVector, Math.max(topK * 10, 50));
        if (!chromaHits.isEmpty()) {
            logCandidates("ChromaDB", question, chromaHits);
            List<RetrievedChunk> approvedHits = approvedDocumentChunks(question, chromaHits, topK);
            if (!approvedHits.isEmpty()) {
                log.info("RAG retrieval source=ChromaDB question='{}' results={}", question,
                    approvedHits.stream().map(chunk -> formatResult(question, chunk)).toList());
                return approvedHits;
            }
        }
        List<RetrievedChunk> fallbackHits = localSearch(queryVector, question, topK);
        log.info("RAG retrieval source=MySQL-fallback question='{}' results={}", question,
            fallbackHits.stream().map(chunk -> formatResult(question, chunk)).toList());
        return fallbackHits;
    }

    private List<RetrievedChunk> localSearch(float[] queryVector, String question, int topK) {
        List<KnowledgeChunk> all = chunkRepository.findAll();
        List<RetrievedChunk> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : all) {
            float[] vector = embeddingService.hashEmbed(chunk.getContent());
            double cosine = EmbeddingService.cosine(queryVector.length == vector.length ? queryVector : embeddingService.hashEmbed(question), vector);
            double lexical = lexicalOverlap(question, chunk.getContent());
            double specificLexical = specificLexicalOverlap(question, chunk.getContent() + " " + chunk.getSourceName());
            double intent = intentAlignment(question, chunk.getSourceName(), chunk.getContent());
            double score = (cosine * 0.25) + (lexical * 0.25) + (specificLexical * 0.25) + (intent * 0.25);
            scored.add(new RetrievedChunk(chunk.getContent(), chunk.getSourceName(), score, chunk.getDocument().getId()));
        }
        return approvedDocumentChunks(question, scored, topK);
    }

    private List<RetrievedChunk> approvedDocumentChunks(String question, List<RetrievedChunk> candidates, int topK) {
        List<RetrievedChunk> approved = candidates.stream()
                .filter(c -> isApprovedMatch(question, c))
                .sorted(Comparator
                    .comparingDouble((RetrievedChunk c) -> rankingScore(question, c)).reversed()
                        .thenComparing(Comparator.comparingDouble(RetrievedChunk::similarity).reversed()))
                .toList();
        if (approved.isEmpty()) {
            return List.of();
        }

        RetrievedChunk best = approved.get(0);
        return approved.stream()
                .filter(c -> sameDocument(c, best))
                .limit(topK)
                .toList();
    }

    private boolean sameDocument(RetrievedChunk first, RetrievedChunk second) {
        if (first.documentId() != null && second.documentId() != null) {
            return first.documentId().equals(second.documentId());
        }
        return first.sourceName().equals(second.sourceName());
    }

    private boolean isApprovedMatch(String question, RetrievedChunk chunk) {
        if (chunk == null || chunk.content() == null || chunk.sourceName() == null) {
            return false;
        }
        if (confidenceScore(question, chunk) < properties.getRag().getMinSimilarity()) {
            return false;
        }

        String searchableDocument = chunk.content() + " " + chunk.sourceName();
        double contentOverlap = lexicalOverlap(question, chunk.content());
        double documentOverlap = specificLexicalOverlap(question, searchableDocument);
        double intentScore = intentAlignment(question, chunk.sourceName(), chunk.content());
        double relevance = relevanceScore(question, chunk);

        return contentOverlap >= 0.15
                && documentOverlap >= 0.2
                && intentScore >= 0.5
                && relevance >= 0.25
                && hasMeaningfulTermMatch(question, searchableDocument);
    }

    private double relevanceScore(String question, RetrievedChunk chunk) {
        double lexical = lexicalOverlap(question, chunk.content());
        double document = specificLexicalOverlap(question, chunk.content() + " " + chunk.sourceName());
        double intent = intentAlignment(question, chunk.sourceName(), chunk.content());
        return (document * 0.45) + (lexical * 0.35) + (intent * 0.2);
    }

    private double rankingScore(String question, RetrievedChunk chunk) {
        return (chunk.similarity() * 0.6) + (relevanceScore(question, chunk) * 0.4);
    }

    private double confidenceScore(String question, RetrievedChunk chunk) {
        return Math.max(chunk.similarity(), relevanceScore(question, chunk));
    }

    private double intentAlignment(String question, String sourceName, String content) {
        String normalizedQuestion = normalizeIntentText(question);
        String normalizedSource = normalizeIntentText(sourceName);
        String searchable = normalizeIntentText(content);
        if (normalizedQuestion.isBlank() || searchable.isBlank()) {
            return 0.0;
        }

        Set<String> questionIntents = matchingIntents(normalizedQuestion);
        Set<String> sourceIntents = matchingIntents(normalizedSource);
        if (!questionIntents.isEmpty()) {
            if (sourceIntents.isEmpty()) {
                sourceIntents = matchingIntents(searchable);
            }
            return questionIntents.stream().anyMatch(sourceIntents::contains) ? 1.0 : 0.0;
        }

        double bestMatch = 0.0;
        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            List<String> keywords = entry.getValue();
            boolean documentMatches = keywords.stream().anyMatch(keyword -> searchable.contains(normalizeIntentText(keyword)));
            if (documentMatches) bestMatch = Math.max(bestMatch, 0.35);
        }

        long matchingTokens = 0;
        long questionTokens = 0;
        for (String token : EmbeddingService.tokenize(normalizedQuestion)) {
            if (token.length() < 3 || isQuestionWord(token)) {
                continue;
            }
            questionTokens++;
            if (searchable.contains(token)) {
                matchingTokens++;
            }
        }
        return questionTokens == 0 ? 0.0 : (double) matchingTokens / questionTokens;
    }

    private Set<String> matchingIntents(String text) {
        return INTENT_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(keyword -> text.contains(normalizeIntentText(keyword))))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String normalizeIntentText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double lexicalOverlap(String question, String content) {
        String[] q = EmbeddingService.tokenize(EmbeddingService.semanticText(question));
        String hay = EmbeddingService.semanticText(content);
        int hits = 0;
        int considered = 0;
        for (String token : q) {
            if (token.length() < 3 || isQuestionWord(token)) continue;
            considered++;
            if (hay.contains(token)) hits++;
        }
        return considered == 0 ? 0 : (double) hits / considered;
    }

    private boolean isQuestionWord(String token) {
        return switch (token) {
            case "how", "what", "when", "where", "which", "who", "why", "can", "could", "would", "should",
                    "do", "does", "did", "is", "are", "my", "the", "a", "an", "to", "i", "we", "you", "please",
                    "help", "company", "problem", "problems", "issue", "issues", "troubleshoot", "troubleshooting",
                    "working", "work", "connect", "connecting", "connection", "connections", "cannot", "unable",
                    "not", "failed", "failure", "broken" -> true;
            default -> false;
        };
    }

    private double specificLexicalOverlap(String question, String content) {
        String[] tokens = EmbeddingService.tokenize(question.toLowerCase());
        String hay = content.toLowerCase();
        int hits = 0;
        int considered = 0;
        for (String token : tokens) {
            if (token.length() < 3 || isQuestionWord(token)) continue;
            considered++;
            if (hay.contains(token)) hits++;
        }
        return considered == 0 ? 0 : (double) hits / considered;
    }

    private boolean hasMeaningfulTermMatch(String question, String content) {
        String searchable = content.toLowerCase();
        for (String token : EmbeddingService.tokenize(question.toLowerCase())) {
            if (token.length() >= 3 && !isQuestionWord(token) && searchable.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String formatResult(String question, RetrievedChunk chunk) {
        return chunk.sourceName() + "(score=" + String.format("%.3f", chunk.similarity())
                + ", relevance=" + String.format("%.3f", relevanceScore(question, chunk)) + ")";
        }

        private void logCandidates(String source, String question, List<RetrievedChunk> candidates) {
        log.info("RAG candidates source={} question='{}' candidates={}", source, question,
            candidates.stream()
                .map(chunk -> chunk.sourceName() + "(score=" + String.format("%.3f", chunk.similarity())
                    + ", content=\"" + compact(chunk.content()) + "\")")
                .toList());
        }

        private String compact(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
    }

    public boolean sufficient(String question, List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return false;
        }
        return isApprovedMatch(question, chunks.get(0));
    }
}