package com.helpdesk.itagent.rag;

import com.helpdesk.itagent.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final AppProperties properties;

    public TextChunker(AppProperties properties) {
        this.properties = properties;
    }

    public List<String> chunk(String raw) {
        String text = raw.replace("\r", " ").replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        int size = properties.getRag().getChunkSize();
        int overlap = properties.getRag().getChunkOverlap();
        List<String> chunks = new ArrayList<>();
        if (text.isBlank()) {
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            if (end < text.length()) {
                int period = text.lastIndexOf('.', end);
                if (period > start + size / 2) {
                    end = period + 1;
                }
            }
            String piece = text.substring(start, end).trim();
            if (!piece.isBlank()) {
                chunks.add(piece);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }
}
