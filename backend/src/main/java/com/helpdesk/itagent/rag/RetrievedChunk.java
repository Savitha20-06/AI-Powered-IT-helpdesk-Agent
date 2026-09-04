package com.helpdesk.itagent.rag;

public record RetrievedChunk(String content, String sourceName, double similarity, Long documentId) {}
