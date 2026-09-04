package com.helpdesk.itagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Chroma chroma = new Chroma();
    private final OpenAi openai = new OpenAi();
    private final Rag rag = new Rag();
    private final Storage storage = new Storage();

    public Jwt getJwt() { return jwt; }
    public Cors getCors() { return cors; }
    public Chroma getChroma() { return chroma; }
    public OpenAi getOpenai() { return openai; }
    public Rag getRag() { return rag; }
    public Storage getStorage() { return storage; }

    public static class Jwt {
        private String secret;
        private long expirationMs;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class Cors {
        private String allowedOrigins;
        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Chroma {
        private String url;
        private String collection;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = collection; }
    }

    public static class OpenAi {
        private String apiKey;
        private String baseUrl;
        private String chatModel;
        private String embeddingModel;
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public static class Rag {
        private int chunkSize;
        private int chunkOverlap;
        private int topK;
        private double minSimilarity;
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getMinSimilarity() { return minSimilarity; }
        public void setMinSimilarity(double minSimilarity) { this.minSimilarity = minSimilarity; }
    }

    public static class Storage {
        private String uploadDir;
        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    }
}
