package com.helpdesk.itagent.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.helpdesk.itagent.config.AppProperties;

@Component
@SuppressWarnings("null")
public class ChromaClient {

    private static final Logger log = LoggerFactory.getLogger(ChromaClient.class);

    private final AppProperties properties;
    private final RestClient restClient;
    private volatile String collectionId;

    public ChromaClient(AppProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public boolean isReachable() {
        try {
            restClient.get()
                    .uri(properties.getChroma().getUrl() + "/api/v1/heartbeat")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("ChromaDB is not reachable at {}: {}", properties.getChroma().getUrl(), ex.getMessage());
            return false;
        }
    }

    public void ensureCollection() {
        if (!isReachable()) {
            return;
        }
        try {
            Map<String, Object> existing = restClient.get()
                    .uri(properties.getChroma().getUrl() + "/api/v1/collections/" + properties.getChroma().getCollection())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (existing != null && existing.get("id") != null) {
                collectionId = String.valueOf(existing.get("id"));
                return;
            }
        } catch (Exception ignored) {
        }
        Map<String, Object> body = new HashMap<>();
        body.put("name", properties.getChroma().getCollection());
        body.put("get_or_create", true);
        body.put("metadata", Map.of("hnsw:space", "cosine"));
        try {
            Map<String, Object> created = restClient.post()
                    .uri(properties.getChroma().getUrl() + "/api/v1/collections")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (created != null && created.get("id") != null) {
                collectionId = String.valueOf(created.get("id"));
            }
        } catch (Exception ex) {
            log.warn("Could not create Chroma collection: {}", ex.getMessage());
        }
    }

    public void upsert(List<String> ids, List<float[]> embeddings, List<String> documents, List<Map<String, Object>> metadatas) {
        if (!isReachable() || ids.isEmpty()) {
            return;
        }
        ensureCollection();
        if (collectionId == null) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("embeddings", toLists(embeddings));
        body.put("documents", documents);
        body.put("metadatas", metadatas);
        restClient.post()
                .uri(properties.getChroma().getUrl() + "/api/v1/collections/" + collectionId + "/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteByDocumentId(Long documentId) {
        if (!isReachable()) {
            return;
        }
        ensureCollection();
        if (collectionId == null) {
            return;
        }
        Map<String, Object> body = Map.of("where", Map.of("documentId", documentId.intValue()));
        try {
            restClient.post()
                    .uri(properties.getChroma().getUrl() + "/api/v1/collections/" + collectionId + "/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Chroma delete failed: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<RetrievedChunk> query(float[] embedding, int topK) {
        if (!isReachable()) {
            return List.of();
        }
        ensureCollection();
        if (collectionId == null) {
            return List.of();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(toList(embedding)));
        body.put("n_results", topK);
        body.put("include", List.of("documents", "metadatas", "distances"));
        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(properties.getChroma().getUrl() + "/api/v1/collections/" + collectionId + "/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Chroma query failed: {}", ex.getMessage());
            return List.of();
        }
        if (response == null) {
            return List.of();
        }
        List<List<String>> documents = (List<List<String>>) response.get("documents");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) response.get("metadatas");
        List<List<Number>> distances = (List<List<Number>>) response.get("distances");
        List<RetrievedChunk> results = new ArrayList<>();
        if (documents == null || documents.isEmpty()) {
            return results;
        }
        List<String> docs = documents.get(0);
        List<Map<String, Object>> metas = metadatas == null || metadatas.isEmpty() ? List.of() : metadatas.get(0);
        List<Number> dists = distances == null || distances.isEmpty() ? List.of() : distances.get(0);
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i) == null || i >= metas.size() || metas.get(i) == null) {
                continue;
            }
            double distance = i < dists.size() && dists.get(i) != null ? dists.get(i).doubleValue() : 1.0;
            double similarity = Math.max(0.0, Math.min(1.0, 1.0 - distance));
            Map<String, Object> meta = metas.get(i);
            Object rawSource = meta.get("sourceName");
            if (!(rawSource instanceof String source) || source.isBlank()) {
                continue;
            }
            Long docId = null;
            Object rawId = meta.get("documentId");
            if (rawId instanceof Number n) {
                docId = n.longValue();
            }
            results.add(new RetrievedChunk(docs.get(i), source, similarity, docId));
        }
        return results;
    }

    private List<List<Float>> toLists(List<float[]> embeddings) {
        List<List<Float>> out = new ArrayList<>();
        for (float[] embedding : embeddings) {
            out.add(toList(embedding));
        }
        return out;
    }

    private List<Float> toList(float[] embedding) {
        List<Float> list = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            list.add(v);
        }
        return list;
    }
}
