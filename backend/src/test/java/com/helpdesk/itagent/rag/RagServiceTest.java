package com.helpdesk.itagent.rag;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.web.client.RestClient;

import com.helpdesk.itagent.config.AppProperties;
import com.helpdesk.itagent.domain.KnowledgeChunk;
import com.helpdesk.itagent.domain.KnowledgeDocument;
import com.helpdesk.itagent.repo.KnowledgeChunkRepository;

class RagServiceTest {

    private RagService ragService;
    private KnowledgeChunkRepository chunkRepository;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getRag().setTopK(5);
        properties.getRag().setMinSimilarity(0.55);
        EmbeddingService embeddingService = new EmbeddingService(properties, null);
        ChromaClient chromaClient = new ChromaClient(properties, RestClient.builder()
            .baseUrl("http://localhost:1")
            .build());
        chunkRepository = mock(KnowledgeChunkRepository.class);
        when(chunkRepository.findAll()).thenReturn(List.of(
                chunk(1L, "vpn-access-guide.txt", "Cisco Secure Client VPN gateway connection and MFA troubleshooting steps for company email users."),
                chunk(2L, "email-outlook-guide.txt", "Company email and Outlook send and receive troubleshooting steps."),
                chunk(3L, "network-wifi-guide.txt", "Company Wi-Fi connection, intranet access, and network troubleshooting steps."),
                chunk(4L, "printer-guide.txt", "Company printer setup and printer problem troubleshooting steps."),
                chunk(5L, "software-install-guide.txt", "Approved company software installation from Software Center."),
                chunk(6L, "password-mfa-guide.txt", "Company password reset and MFA account troubleshooting steps.")));
        ragService = new RagService(properties, embeddingService, chromaClient, chunkRepository);
    }

    @Test
    void retrievesOnlyTheRelevantDocumentForSupportedQuestions() {
        assertOnlySource("My VPN is not connecting", "vpn-access-guide.txt");
        assertOnlySource("My company email is not working", "email-outlook-guide.txt");
        assertOnlySource("I cannot connect to company Wi-Fi", "network-wifi-guide.txt");
        assertOnlySource("I cannot access the company intranet", "network-wifi-guide.txt");
        assertOnlySource("How can I troubleshoot a printer problem?", "printer-guide.txt");
        assertOnlySource("How do I install approved company software?", "software-install-guide.txt");
        assertOnlySource("How do I reset my password?", "password-mfa-guide.txt");
    }

    @Test
    void onlyMarksARelevantChunkAsSufficient() {
        List<RetrievedChunk> printer = ragService.retrieve("How do I install approved company software?");
        assertThat(ragService.sufficient("How do I install approved company software?", printer)).isTrue();
        assertThat(ragService.sufficient("Which is the capital of India?", printer)).isFalse();
    }

    @Test
    void rejectsAClosestButInsufficientDocument() {
        assertThat(ragService.retrieve("Which is the capital of India?")).isEmpty();
    }

    private void assertOnlySource(String question, String expectedSource) {
        List<RetrievedChunk> results = ragService.retrieve(question);
        assertThat(results).as("retrieval for %s", question).isNotEmpty();
        assertThat(results).as("source for %s", question).allMatch(chunk -> expectedSource.equals(chunk.sourceName()));
    }

    private KnowledgeChunk chunk(Long documentId, String source, String content) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(documentId);
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocument(document);
        chunk.setSourceName(source);
        chunk.setContent(content);
        return chunk;
    }
}
