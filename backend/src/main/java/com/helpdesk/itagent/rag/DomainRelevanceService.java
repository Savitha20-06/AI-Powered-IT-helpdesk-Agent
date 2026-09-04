package com.helpdesk.itagent.rag;

import java.util.Arrays;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class DomainRelevanceService {

    private static final Set<String> IT_TERMS = Set.of(
            "access", "account", "application", "computer", "email", "error", "hardware",
            "internet", "it", "keyboard", "laptop", "login", "mfa", "monitor", "network",
            "office", "outlook", "password", "permission", "printer", "software", "teams",
            "ticket", "vpn", "wifi", "wireless", "anyconnect", "tunnel", "gateway", "remote", "remotely"
    );

    public boolean isCompanyItQuestion(String question) {
        return Arrays.stream(EmbeddingService.tokenize(question))
                .map(token -> token.replaceAll("[^a-z0-9]", ""))
                .anyMatch(IT_TERMS::contains);
    }
}