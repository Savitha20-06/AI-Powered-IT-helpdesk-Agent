package com.helpdesk.itagent.rag;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.itagent.config.AppProperties;
import com.helpdesk.itagent.domain.IssueCategory;
import com.helpdesk.itagent.domain.TicketPriority;

@Service
@SuppressWarnings("null")
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LlmService(AppProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public AiDecision answer(String question, List<RetrievedChunk> chunks, boolean sufficientKnowledge) {
        if (!sufficientKnowledge || chunks.isEmpty()) {
            return groundedFallback(question, chunks, false);
        }
        if (properties.getOpenai().isConfigured()) {
            try {
                return callOpenAi(question, chunks, sufficientKnowledge);
            } catch (Exception ex) {
                log.warn("LLM call failed, using grounded fallback: {}", ex.getMessage());
            }
        }
        return groundedFallback(question, chunks, sufficientKnowledge);
    }

    private AiDecision callOpenAi(String question, List<RetrievedChunk> chunks, boolean sufficientKnowledge) throws Exception {
        String context = chunks.stream()
                .map(c -> "SOURCE: " + c.sourceName() + "\n" + c.content())
                .collect(Collectors.joining("\n\n---\n\n"));
        String system = """
                You are the enterprise IT helpdesk assistant for one company.
                Answer ONLY using the provided knowledge-base context.
                Never invent policies, URLs, passwords, or steps that are not in the context.
                If the context is missing or insufficient, set sufficientKnowledge=false and recommend escalation.
                Classify the issue into one of: HARDWARE, SOFTWARE, NETWORK, VPN, EMAIL, PASSWORD, PRINTER, ACCESS, OTHER.
                Suggest priority: LOW, MEDIUM, HIGH, CRITICAL.
                Return compact JSON with keys:
                answer, sufficientKnowledge, category, priority, confidence, summary, symptoms, troubleshootingAttempted.
                """;
        String user = """
                Sufficient retrieval flag from the system: %s
                Employee question:
                %s

                Knowledge-base context:
                %s
                """.formatted(sufficientKnowledge, question, context.isBlank() ? "(no matching documents)" : context);

        Map<String, Object> body = Map.of(
                "model", properties.getOpenai().getChatModel(),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                )
        );
        Map<?, ?> response = restClient.post()
                .uri(properties.getOpenai().getBaseUrl() + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
            if (response == null) {
                throw new IllegalStateException("Empty chat completion response");
            }
        List<?> choices = (List<?>) response.get("choices");
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        String content = String.valueOf(message.get("content"));
        return parseDecision(content, sufficientKnowledge, question, chunks);
    }

    private AiDecision parseDecision(String json, boolean retrievalOk, String question, List<RetrievedChunk> chunks) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        boolean sufficient = retrievalOk && node.path("sufficientKnowledge").asBoolean(false);
        IssueCategory category = parseCategory(node.path("category").asText("OTHER"));
        TicketPriority priority = parsePriority(node.path("priority").asText("MEDIUM"));
        String answer = node.path("answer").asText("");
        if (!sufficient) {
            answer = escalationAnswer(chunks);
        }
        return new AiDecision(
                answer,
                sufficient,
                category,
                priority,
                node.path("confidence").asDouble(sufficient ? 0.75 : 0.35),
                node.path("summary").asText(question),
                node.path("symptoms").asText(question),
                node.path("troubleshootingAttempted").asText("Asked the AI helpdesk assistant using the approved knowledge base.")
        );
    }

    private AiDecision groundedFallback(String question, List<RetrievedChunk> chunks, boolean sufficientKnowledge) {
        IssueCategory category = classify(question);
        TicketPriority priority = suggestPriority(question, category);
        if (!sufficientKnowledge || chunks.isEmpty()) {
            return new AiDecision(
                    escalationAnswer(chunks),
                    false,
                    category,
                    priority,
                    0.28,
                    summarize(question),
                    question,
                    "Employee asked the AI assistant. No sufficiently matching approved knowledge-base article was found."
            );
        }
        StringBuilder answer = new StringBuilder();
        answer.append("Based on the approved knowledge base, here is the recommended guidance:\n\n");
        RetrievedChunk top = chunks.get(0);
        answer.append(top.content().length() > 900 ? top.content().substring(0, 900) + "..." : top.content());
        answer.append("\n\nSource: ").append(top.sourceName());
        answer.append("\n\nIf this does not resolve the issue, ask IT Staff to take over and a ticket can be created.");
        return new AiDecision(
                answer.toString(),
                true,
                category,
                priority,
                Math.min(0.92, 0.55 + top.similarity()),
                summarize(question),
                question,
                "Employee consulted the AI helpdesk. Relevant knowledge-base article(s) were returned."
        );
    }

    private String escalationAnswer(List<RetrievedChunk> chunks) {
        String extra = chunks.isEmpty()
                ? "No approved article matched this question closely enough."
                : "Related articles were only a weak match, so I will not guess.";
        return "I do not have enough approved knowledge-base information to answer this safely. "
                + extra
                + " I am escalating this to IT Support and creating a ticket with the problem summary, symptoms, and suggested priority.";
    }

    public IssueCategory classify(String text) {
        String q = text.toLowerCase(Locale.ROOT);
        if (q.contains("vpn") || q.contains("anyconnect") || q.contains("remote access client")) return IssueCategory.VPN;
        if (q.contains("password") || q.contains("reset") || q.contains("locked out") || q.contains("mfa") || q.contains("otp")) return IssueCategory.PASSWORD;
        if (q.contains("outlook") || q.contains("email") || q.contains("mailbox") || q.contains("owa")) return IssueCategory.EMAIL;
        if (q.contains("printer") || q.contains("print") || q.contains("toner")) return IssueCategory.PRINTER;
        if (q.contains("wifi") || q.contains("network") || q.contains("ethernet") || q.contains("internet") || q.contains("dns")) return IssueCategory.NETWORK;
        if (q.contains("laptop") || q.contains("keyboard") || q.contains("monitor") || q.contains("battery") || q.contains("hardware")) return IssueCategory.HARDWARE;
        if (q.contains("access") || q.contains("permission") || q.contains("sharepoint") || q.contains("ad account") || q.contains("role")) return IssueCategory.ACCESS;
        if (q.contains("software") || q.contains("install") || q.contains("office") || q.contains("teams") || q.contains("license")) return IssueCategory.SOFTWARE;
        return IssueCategory.OTHER;
    }

    public TicketPriority suggestPriority(String text, IssueCategory category) {
        String q = text.toLowerCase(Locale.ROOT);
        if (q.contains("outage") || q.contains("entire team") || q.contains("ransomware") || q.contains("cannot work at all") || q.contains("production down")) {
            return TicketPriority.CRITICAL;
        }
        if (category == IssueCategory.VPN || category == IssueCategory.PASSWORD || category == IssueCategory.EMAIL || q.contains("blocked") || q.contains("urgent")) {
            return TicketPriority.HIGH;
        }
        if (category == IssueCategory.PRINTER || category == IssueCategory.HARDWARE) {
            return TicketPriority.MEDIUM;
        }
        return TicketPriority.LOW;
    }

    private IssueCategory parseCategory(String raw) {
        try {
            return IssueCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return IssueCategory.OTHER;
        }
    }

    private TicketPriority parsePriority(String raw) {
        try {
            return TicketPriority.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return TicketPriority.MEDIUM;
        }
    }

    private String summarize(String question) {
        String trimmed = question.trim();
        return trimmed.length() <= 180 ? trimmed : trimmed.substring(0, 177) + "...";
    }
}
