package com.helpdesk.itagent.rag;

import com.helpdesk.itagent.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void returnsNoChunksForBlankInput() {
        TextChunker chunker = new TextChunker(properties(20, 5));

        assertThat(chunker.chunk("  \r\n\t  ")).isEmpty();
    }

    @Test
    void normalizesWhitespaceAndKeepsSentenceBoundary() {
        TextChunker chunker = new TextChunker(properties(24, 0));

        List<String> chunks = chunker.chunk("First   sentence. Second sentence.");

        assertThat(chunks).containsExactly("First sentence.", "Second sentence.");
    }

    @Test
    void createsOverlappingChunksWhenTextExceedsChunkSize() {
        TextChunker chunker = new TextChunker(properties(10, 3));

        List<String> chunks = chunker.chunk("0123456789abcdefghij");

        assertThat(chunks).containsExactly("0123456789", "789abcdefg", "efghij");
    }

    private static AppProperties properties(int chunkSize, int chunkOverlap) {
        AppProperties properties = new AppProperties();
        properties.getRag().setChunkSize(chunkSize);
        properties.getRag().setChunkOverlap(chunkOverlap);
        return properties;
    }
}
