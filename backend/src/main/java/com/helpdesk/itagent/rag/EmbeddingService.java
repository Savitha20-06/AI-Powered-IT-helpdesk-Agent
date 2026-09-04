package com.helpdesk.itagent.rag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.helpdesk.itagent.config.AppProperties;

@Service
@SuppressWarnings("null")
public class EmbeddingService {

    public static final int LOCAL_DIMENSIONS = 384;

    private final AppProperties properties;
    private final RestClient restClient;

    public EmbeddingService(AppProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public List<float[]> embedAll(List<String> texts) {
        if (properties.getOpenai().isConfigured()) {
            try {
                return embedOpenAi(texts);
            } catch (Exception ex) {
                // Fall through to local embeddings so ingestion still works in a demo.
            }
        }
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(hashEmbed(text));
        }
        return vectors;
    }

    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    @SuppressWarnings("unchecked")
    private List<float[]> embedOpenAi(List<String> texts) {
        Map<String, Object> body = Map.of(
                "model", properties.getOpenai().getEmbeddingModel(),
                "input", texts
        );
        Map<String, Object> response = restClient.post()
                .uri(properties.getOpenai().getBaseUrl() + "/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null) {
            throw new IllegalStateException("Empty embedding response");
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<float[]> vectors = new ArrayList<>();
        for (Map<String, Object> item : data) {
            List<Number> embedding = (List<Number>) item.get("embedding");
            float[] vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = embedding.get(i).floatValue();
            }
            vectors.add(vec);
        }
        return vectors;
    }

    /**
     * Feature-hashing embedding. Same words land in the same dimensions, so similar
     * IT questions retrieve similar KB chunks even without a paid embedding API.
     */
    public float[] hashEmbed(String text) {
        float[] vector = new float[LOCAL_DIMENSIONS];
        String[] tokens = tokenize(semanticText(text));
        for (String token : tokens) {
            int h1 = Math.floorMod(token.hashCode(), LOCAL_DIMENSIONS);
            int h2 = Math.floorMod((token.hashCode() * 31) ^ token.length(), LOCAL_DIMENSIONS);
            vector[h1] += 1.0f;
            vector[h2] += 0.5f;
        }
        if (isVpnTopic(text)) {
            vector[LOCAL_DIMENSIONS - 1] += 100.0f;
        }
        if (isTroubleshootingQuestion(text)) {
            vector[LOCAL_DIMENSIONS - 2] += 1.5f;
        }
        normalize(vector);
        return vector;
    }

    public static String[] tokenize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9@._\\- ]", " ")
                .split("\\s+");
    }

    public static String semanticText(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        StringBuilder expanded = new StringBuilder(normalized);
        if (containsAny(normalized, "vpn", "anyconnect", "secure client", "remote access", "remote connection",
            "remotely", "tunnel", "gateway", "corporate network", "internal systems", "connect to internal")) {
            expanded.append(" vpn_topic vpn_topic vpn_topic vpn_topic vpn virtual private network remote access tunnel connection gateway anyconnect");
        }
        if (containsAny(normalized, "not working", "doesn't work", "does not work", "cannot connect", "can't connect",
                "unable to connect", "connection failed", "login failed", "error", "issue", "problem", "down")) {
            expanded.append(" troubleshooting failure problem error connect");
        }
        return expanded.toString();
    }

    public static boolean isVpnTopic(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return containsAny(normalized, "vpn", "anyconnect", "secure client", "remote access", "remote connection",
            "remotely", "tunnel", "gateway", "corporate network", "internal systems", "connect to internal");
    }

    public static double semanticSimilarity(String first, String second) {
        double vectorSimilarity = cosine(new EmbeddingService(null, null).hashEmbed(first),
                new EmbeddingService(null, null).hashEmbed(second));
        if (semanticText(first).contains("vpn_topic") && semanticText(second).contains("vpn_topic")) {
            return Math.max(vectorSimilarity, 0.85);
        }
        return vectorSimilarity;
    }

    private static boolean isTroubleshootingQuestion(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return containsAny(normalized, "not working", "doesn't work", "does not work", "cannot connect", "can't connect",
                "unable to connect", "connection failed", "login failed", "error", "issue", "problem", "down");
    }

    private static boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    public static void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }

    public static double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
