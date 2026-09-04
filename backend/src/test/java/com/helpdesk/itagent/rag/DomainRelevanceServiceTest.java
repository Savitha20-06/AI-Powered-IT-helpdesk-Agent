package com.helpdesk.itagent.rag;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DomainRelevanceServiceTest {

    private final DomainRelevanceService relevanceService = new DomainRelevanceService();

    @Test
    void acceptsCompanyItSupportTopics() {
        assertThat(relevanceService.isCompanyItQuestion("How do I reset my VPN password?"))
                .isTrue();
        assertThat(relevanceService.isCompanyItQuestion("My remote connection is not working"))
            .isTrue();
        assertThat(relevanceService.isCompanyItQuestion("My Outlook email is not syncing."))
                .isTrue();
    }

    @Test
    void rejectsUnrelatedGeneralKnowledgeQuestions() {
        assertThat(relevanceService.isCompanyItQuestion("Which is the capital of India?"))
                .isFalse();
    }

    @Test
    void rejectsUnrelatedQuestionsWithoutItTerms() {
        assertThat(relevanceService.isCompanyItQuestion("How do I cook pasta?"))
                .isFalse();
    }
}
